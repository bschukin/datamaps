package com.datamaps.services

import com.datamaps.mappings.DataMappingsService
import com.datamaps.mappings.IdGenerationType
import com.datamaps.maps.DataMap
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.ConversionService
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.util.LinkedCaseInsensitiveMap
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
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

    @Autowired
    lateinit var dmUtilService: DmUtilService

    @Resource
    lateinit var sequenceIncrementor: SequenceIncrementor

    @Resource
    lateinit var dbDialect: DbDialect

    @PostConstruct
    fun init() {
        DeltaStore.deltaMachine = this
    }

    fun flush() {
        val buckets = DeltaStore.flushToBuckets()

        createAndExeUpdateStatements(buckets)

        buckets.filter { it.dm.isNew() }.forEach { it.dm.persisted() }
    }

    internal fun createAndExeUpdateStatements(buckets: List<DeltaBucket>): List<Pair<String, Map<String, Any?>>> {
        return buckets.stream().map { b ->
            when {
                b.isDelete() -> createDelete(b)
                b.isUpdate() -> createUpdate(b)
                else -> createInsert(b)
            }
        }.toList()
    }

    private fun createDelete(db: DeltaBucket): Pair<String, Map<String, Any?>> {
        val mapping = dataMappingsService.getDataMapping(db.dm.entity)
        val sql = "DELETE FROM ${mapping.table} WHERE ${mapping.idColumn} = :_id"
        val map = mapOf("_id" to db.dm.id)

        LOGGER.info("\r\ndml: ${sql} \n\t with params ${map}")

        namedParameterJdbcTemplate.update(sql, map)

        return Pair(sql, map)
    }

    private fun createUpdate(db: DeltaBucket): Pair<String, Map<String, Any?>> {


        val mapping = dataMappingsService.getDataMapping(db.dm.entity)
        var sql = "UPDATE ${mapping.table} SET"
        val map = mutableMapOf<String, Any?>()

        sql +=
                db.deltas.values.joinToString { delta ->
                    //кладеим в мапу параметр
                    when (delta.newValue) {
                        is DataMap -> map["_${delta.property}"] = delta.newValue.id
                        else -> map["_${delta.property}"] = delta.newValue
                    }

                    //возваращаем текущий клауз SET
                    " ${dbDialect.getQuotedDbIdentifier(mapping[delta.property].sqlcolumn)} = :_${delta.property}"

                }

        sql += " \n WHERE ${mapping.idColumn} = :_ID"
        map["_ID"] = db.dm.id

        LOGGER.info("\r\ndml: ${sql} \n\t with params ${map}")

        namedParameterJdbcTemplate.update(sql, map)

        return Pair(sql, map)
    }

    private fun createInsert(db: DeltaBucket): Pair<String, Map<String, Any?>> {
        val mapping = dataMappingsService.getDataMapping(db.dm.entity)
        var sql = "INSERT INTO ${mapping.table} ("
        val map = LinkedHashMap<String, Any?>()

        if (mapping.idGenerationType == IdGenerationType.SEQUENCE) {
            db.dm.id = sequenceIncrementor.getNextId(mapping.name)
            db.deltas["id"] = Delta(DeltaType.VALUE_CHANGE, db.dm, "id", null, db.dm.id)
        }


        sql += db.deltas.values.joinToString { delta ->
            "${dbDialect.getQuotedDbIdentifier(mapping[delta.property].sqlcolumn)}"
        }
        sql += ") VALUES ("
        sql += db.deltas.values.joinToString { _ -> "?" }
        sql += ")"

        db.deltas.values.forEach { delta ->
            when (delta.newValue) {
                is DataMap -> map["_${delta.property}"] = delta.newValue.id
                else -> map["_${delta.property}"] = delta.newValue
            }
        }

        LOGGER.info("\r\ndml: ${sql} \n\t with params ${map}")

        val holder = GeneratedKeyHolder()
        namedParameterJdbcTemplate.jdbcOperations.update(object : PreparedStatementCreator {

            @Throws(SQLException::class)
            override fun createPreparedStatement(connection: Connection): PreparedStatement {
                val ps = connection.prepareStatement(sql,
                        Statement.RETURN_GENERATED_KEYS)
                var i = 1
                map.forEach { t, u -> ps.setObject(i++, u) }
                return ps
            }
        }, holder)

        if (db.dm.id == null)
            db.dm.id = holder.keys["id"]

        return Pair(sql, map)
    }

    fun updateBackRef(parent: DataMap, child: DataMap, property: String) {
        dmUtilService.updateBackRef(parent, child, property, false)
    }


}


object DeltaStore {

    internal var context = ThreadLocal<TransactionContext>()
    internal lateinit var deltaMachine: DeltaMachine

    fun delta(dm: DataMap, property: String, oldValue: Any?, newValue: Any?) {
        if (notInTransaction()) return
        context.get().deltas.add(Delta(DeltaType.VALUE_CHANGE, dm, property, oldValue, newValue))

    }

    fun create(dm: DataMap) {
        if (notInTransaction()) return
        context.get().deltas.add(Delta(DeltaType.CREATE, dm))

    }

    fun delete(dm: DataMap) {
        if (notInTransaction()) return
        context.get().deltas.add(Delta(DeltaType.DELETE, dm))

    }

    fun listAdd(parent: DataMap, child: DataMap, property: String) {
        if (notInTransaction()) return
        deltaMachine.updateBackRef(parent, child, property)
    }

    fun listRemove(parent: DataMap, child: DataMap, property: String) {
        if (notInTransaction()) return
        context.get().deltas.add(Delta(DeltaType.DELETE, child))
    }

    private fun notInTransaction(): Boolean {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            return true

        if (context.get() == null) {
            context.set(startTransactionContext())
        }
        return false
    }

    private fun startTransactionContext(): TransactionContext {


        val tsa = TransactionSynchronizationAdapter()

        TransactionSynchronizationManager.registerSynchronization(tsa)

        return TransactionContext(mutableListOf(), tsa)
    }

    private fun clearTransactionContext() {

        if (context.get() != null) {
            context.get().deltas.clear()
        }
    }

    //отправляет все накопленные изменения в базу
    internal fun flushToBuckets(): List<DeltaBucket> {
        //создаем бакеты: наборы изменений по одному объекту
        //один букет - одна DML ооперация
        val buckets = collectBuckets()

        clearTransactionContext()

        return buckets
    }


    internal fun collectBuckets(): List<DeltaBucket> {
        if (context.get() == null)
            return emptyList()

        var lastDM: DataMap? = null
        var currBucket: DeltaBucket? = null
        val res = mutableListOf<DeltaBucket>()

        context.get().deltas.forEach { delta ->
            if (delta.type == DeltaType.CREATE) {
                lastDM = null
                currBucket = DeltaBucket(delta.dm)
            }
            if (lastDM != delta.dm) {
                currBucket = DeltaBucket(delta.dm)
                lastDM = delta.dm
                res.add(currBucket!!)
            }
            if (delta.type != DeltaType.CREATE)
                currBucket!!.deltas[delta.property] = delta
        }
        return res
    }

    class TransactionSynchronizationAdapter : TransactionSynchronization {

        override fun beforeCompletion() {
            clearTransactionContext()
        }


        override fun beforeCommit(readOnly: Boolean) {
            deltaMachine.flush()
        }
    }
}


internal class TransactionContext(val deltas: MutableList<Delta>, val transSynch: TransactionSynchronization?)


enum class DeltaType {
    CREATE,
    DELETE,
    VALUE_CHANGE,
    ADD_TO_LIST,
    DELETE_FROM_LIST
}

//атомарное изменение
internal data class Delta(val type: DeltaType, val dm: DataMap, val property: String = "",
                          val oldValue: Any? = null, val newValue: Any? = null)


//набор изменений по одному мапу
//один букет - одна DML ооперация
internal data class DeltaBucket(val dm: DataMap, val deltas: LinkedCaseInsensitiveMap<Delta> = LinkedCaseInsensitiveMap()) {

    fun isUpdate() = !dm.isNew()
    fun isDelete() = !deltas.values.isEmpty() && deltas.values.first().type == DeltaType.DELETE

}

