package com.datamaps.services

import com.datamaps.mappings.DataMappingsService
import com.datamaps.maps.DataMap
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.LinkedCaseInsensitiveMap
import javax.annotation.PostConstruct
import javax.annotation.Resource
import kotlin.streams.toList


/*Сервис отвечающий за изменения дата мапсов*/
@Service
class DeltaMachine {

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @Resource
    lateinit var dataMappingsService: DataMappingsService

    @Autowired
    lateinit var namedParameterJdbcTemplate: NamedParameterJdbcTemplate

    @PostConstruct
    fun init() {
        DeltaStore.deltaMachine = this
    }

    fun flush() {
        val buckets = DeltaStore.flushToBuckets()
        val statements = createUpdateStatements(buckets)

        statements.forEach { st ->
            LOGGER.info("\r\ndml: ${st.first} \n\t with params ${st.second}")
            namedParameterJdbcTemplate.update(st.first, st.second)
        }
    }

    internal fun createUpdateStatements(buckets: List<DeltaBucket>): List<Pair<String, Map<String, Any?>>> {
        return buckets.stream().map { b ->
            when (b.isUpdate()) {
                true -> createUpdate(b)
                false -> TODO()
            }
        }.toList()
    }

    private fun createUpdate(db: DeltaBucket): Pair<String, Map<String, Any?>> {
        val mapping = dataMappingsService.getDataMapping(db.dm.entity)
        var sql = "UPDATE ${mapping.table} SET"
        val map = mutableMapOf<String, Any?>()
        db.deltas.values.forEach { delta ->

            sql += " ${mapping[delta.property].sqlcolumn} = :_${delta.property}"

            when (delta.newValue) {
                is DataMap -> map["_${delta.property}"] = delta.newValue.id
                else -> map["_${delta.property}"] = delta.newValue
            }

        }
        sql += " \n WHERE ${mapping.idColumn} = :_ID"
        map["_ID"] = db.dm.id

        return Pair(sql, map)
    }


}


object DeltaStore {

    internal var context = ThreadLocal<TransactionContext>()
    internal lateinit var deltaMachine: DeltaMachine

    fun delta(dm: DataMap, property: String, oldValue: Any?, newValue: Any?) {
        if (context.get() == null) {
            context.set(startTransactionContext())
        }
        context.get().deltas.add(Delta(dm, property, oldValue, newValue))
    }

    private fun startTransactionContext(): TransactionContext {

        val tsa = TransactionSynchronizationAdapter()
        TransactionSynchronizationManager.registerSynchronization(tsa)

        return TransactionContext(mutableListOf(), tsa)
    }

    private fun clearTransactionContext() {

        if (context.get() != null) {
        }
        context.get().deltas.clear()
    }

    //отправляет все накопленные изменения в базу
    internal fun flushToBuckets(): List<DeltaBucket> {
        //создаем бакеты: наборы изменений по одному объекту
        //один букет - одна DML ооперация
        val buckets = collectBuckets()

        clearTransactionContext()

        return buckets
    }

    private fun createUpdateStatements(buckets: Any) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    internal fun collectBuckets(): List<DeltaBucket> {
        var lastDM: DataMap? = null
        var currBucket: DeltaBucket? = null
        val res = mutableListOf<DeltaBucket>()

        context.get().deltas.forEach { delta ->
            if (lastDM != delta.dm) {
                currBucket = DeltaBucket(delta.dm)
                lastDM = delta.dm
                res.add(currBucket!!)
            }
            currBucket!!.deltas[delta.property] = delta
        }
        return res
    }

    class TransactionSynchronizationAdapter : TransactionSynchronization {

        override fun beforeCompletion() {

        }

        override fun beforeCommit(readOnly: Boolean) {
            deltaMachine.flush()
        }
    }
}


internal class TransactionContext(val deltas: MutableList<Delta>, val transSynch: TransactionSynchronization?)


//атомарное изменение
internal data class Delta(val dm: DataMap, val property: String, val oldValue: Any?, val newValue: Any?)


//набор изменений по одному мапу
//один букет - одна DML ооперация
internal data class DeltaBucket(val dm: DataMap, val deltas: LinkedCaseInsensitiveMap<Delta> = LinkedCaseInsensitiveMap()) {

    fun isUpdate() = dm.id != null

}

