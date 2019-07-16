package com.bftcom.ice.datamaps.impl.delta

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.misc.makeSure
import com.bftcom.ice.datamaps.misc.throwImpossible
import com.bftcom.ice.datamaps.impl.dialects.DbDialect
import com.bftcom.ice.datamaps.impl.util.SequenceIncrementor
import com.bftcom.ice.datamaps.impl.query.SqlQueryContext
import com.bftcom.ice.datamaps.impl.dialects.DbExceptionsMessageTranslator
import com.bftcom.ice.datamaps.impl.mappings.DataMapping
import com.bftcom.ice.datamaps.impl.mappings.DataMappingsService
import com.bftcom.ice.datamaps.impl.mappings.IdGenerationType
import com.bftcom.ice.datamaps.impl.util.ConvertHelper
import com.bftcom.ice.datamaps.impl.util.DataMapsUtilService
import com.bftcom.ice.datamaps.impl.util.PreparedStatementCreatorExtd
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.ConversionService
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.support.GeneratedKeyHolder
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct
import kotlin.streams.toList


/*Сервис отвечающий за изменения дата мапсов*/
@Service
class DeltaMachine {

    companion object {
        private val logger = LoggerFactory.getLogger(DeltaMachine::class.java)
    }

    @Autowired
    private lateinit var dataMappingsService: DataMappingsService

    @Autowired
    private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcOperations

    @Autowired
    private lateinit var dataMapsUtilService: DataMapsUtilService

    @Autowired
    private lateinit var sequenceIncrementor: SequenceIncrementor

    @Autowired
    private lateinit var dbDialect: DbDialect

    @Autowired
    private lateinit var conversionService: ConversionService

    @Autowired
    private lateinit var dataMapTriggersRegistry: DataMapTriggersRegistry

    @Autowired
    private lateinit var convertHelper: ConvertHelper

    @Autowired
    private lateinit var dbExceptionsMessageTranslator: DbExceptionsMessageTranslator


    @PostConstruct
    fun init() {
        DeltaStore.deltaMachine = this
    }


    fun insert(dataMap: DataMap, presInsertAction: ((DataMap) -> Unit)?, runTriggers: Boolean = false) {

        if (presInsertAction != null)
            presInsertAction(dataMap)

        val deltas = mutableListOf<Delta>()

        dataMap.map.filter { v -> v.value !is Collection<*> }.forEach { t, u ->
            val d = Delta(DeltaType.VALUE_CHANGE, dataMap, t, null, AnyValue(u))
            deltas.add(d)
        }
        val db = DeltaBucket(dataMap, deltas, true)
        createAndExeUpdateStatements(listOf(db), isClientChange = false, runTriggers = runTriggers)

        //чистим контектс
        DeltaStore.removeChanges(dataMap)
    }

    fun deleteAll(entity: String) {
        val mapping = dataMappingsService.getDataMapping(entity)
        val sql = "DELETE FROM ${mapping.table}"

        logger.warn("\r\ndml: $sql")

        doUpdate(entity, sql, emptyMap())
    }

    fun deleteByProjection(q: SqlQueryContext) {
        doUpdate(q.qr.root.dm.name, q.sql, convertMapValuesForDb(q.params.toMutableMap()))
    }


    fun flush() {

        val buckets = DeltaStore.flushAllToBuckets()

        createAndExeUpdateStatements(buckets, isClientChange = false)

    }

    enum class DmlType { INSERT, UPDATE, DELETE }
    private data class FlushContext(val newids: MutableMap<String, DataMap> = mutableMapOf(),
                                    val operationTypes: IdentityHashMap<DeltaBucket, DmlType> = IdentityHashMap()) {
        fun isInsertedAlready(dm: DataMap): Boolean {
            return !dm.isNew() || newids.containsKey(dm.newMapGuid)
        }
    }

    internal fun createAndExeUpdateStatements(abuckets: List<DeltaBucket>,
                                              isClientChange: Boolean = false,
                                              runTriggers: Boolean = true): List<Pair<String, Map<String, Any?>>> {
        val fc = FlushContext()

        var buckets  = abuckets

        buckets.forEach { b ->
            val isUpdate = fc.isInsertedAlready(b.dm) && !b.forceNew
            when {
                b.isDelete() -> fc.operationTypes[b] = DmlType.DELETE
                isUpdate -> fc.operationTypes[b] = DmlType.UPDATE
                else -> fc.operationTypes[b] = DmlType.INSERT
            }
        }
        if (runTriggers)
            buckets=  dataMapTriggersRegistry.runTriggers(fc.operationTypes, buckets, before = true, isClientChange = isClientChange)

        val res = buckets.stream()
                .filter { it.deltas.isNotEmpty() }  // отфильтровка пустых бакетов полученных в результате работы триггеров
                .map { b ->
                    val dmlType = fc.operationTypes[b]!!
                    val isUpdate = fc.isInsertedAlready(b.dm) && !b.forceNew
                    when {
                        dmlType == DmlType.DELETE -> executeDelete(b)
                        dmlType == DmlType.INSERT && !isUpdate -> executeInsert(b, fc)
                        isUpdate || dmlType == DmlType.UPDATE -> executeUpdate(b, fc)
                        else -> throwImpossible()

                    }
                }.toList()

        if (runTriggers)
            dataMapTriggersRegistry.runTriggers(fc.operationTypes, buckets, before = false, isClientChange = isClientChange)


        return res
    }

    private fun executeDelete(db: DeltaBucket): Pair<String, Map<String, Any?>> {
        val mapping = dataMappingsService.getDataMapping(db.dm.entity)
        val sql = "DELETE FROM ${mapping.table} WHERE ${mapping.idColumn} = :_id"
        val map = mutableMapOf("_id" to db.dm.id)
        convertMapValuesForDb(map)

        logger.info("\r\ndml: $sql \n\t with params $map")

        doUpdate(db.dm.entity, sql, map)

        return Pair(sql, map)
    }

    private fun executeUpdate(db: DeltaBucket, fc: FlushContext): Pair<String, Map<String, Any?>> {

        updateBackRefsInBucket(db)

        val mapping = dataMappingsService.getDataMapping(db.dm.entity)
        var sql = "UPDATE ${mapping.table} SET"
        val map = mutableMapOf<String, Any?>()

        sql +=
                db.deltas.joinToString { delta ->
                    //кладем в мапу параметр
                    val newValue = delta.newValue?.value()
                    map["_${delta.property}"] = extractParam(newValue, mapping, delta, fc)

                    //возваращаем текущий клауз SET
                    " ${dbDialect.getQuotedDbIdentifier(mapping[delta.property!!].sqlcolumn)} = :_${delta.property}"

                }

        sql += " \n WHERE ${mapping.idColumn} = :_ID"
        map["_ID"] = getDataMapId(db.dm, fc)

        convertMapValuesForDb(map)
        logger.info("\r\ndml: $sql \n\t with params $map")

        doUpdate(db.dm.entity, sql, map)

        return Pair(sql, map)
    }


    private fun executeInsert(db: DeltaBucket, fc: FlushContext): Pair<String, Map<String, Any?>> {

        updateBackRefsInBucket(db)

        val mapping = dataMappingsService.getDataMapping(db.dm.entity)
        var sql = "INSERT INTO ${mapping.table} ("
        val map = LinkedHashMap<String, Any?>()

        if (mapping.idGenerationType == IdGenerationType.SEQUENCE) {
            db.dm.id = sequenceIncrementor.getNextId(mapping.name)
            db.addDelta(Delta(DeltaType.VALUE_CHANGE, db.dm, "id", null, AnyValue(db.dm.id)))
        }

        if (mapping.idGenerationType == IdGenerationType.NONE && db.dm.id != null) {
            db.addDelta(Delta(DeltaType.VALUE_CHANGE, db.dm, "id", null, AnyValue(db.dm.id)))
        }

        sql += db.deltas.joinToString { delta ->
            dbDialect.getQuotedDbIdentifier(mapping[delta.property!!].sqlcolumn)
        }
        sql += ") VALUES ("
        sql += db.deltas.joinToString { _ -> "?" }
        sql += ")"

        db.deltas.forEach { delta ->
            val newValue = delta.newValue?.value()
            map["_${delta.property}"] = extractParam(newValue, mapping, delta, fc)
        }

        convertMapValuesForDb(map)
        logger.info("\r\ndml: $sql \n\t with params $map")

        val holder = GeneratedKeyHolder()

        val psc = PreparedStatementCreator { con ->
            val ps = con.prepareStatement(sql,
                    arrayOf(mapping.idColumn))
            var i = 1
            map.forEach { _, u -> ps.setObject(i++, u) }
            ps
        }

        doInsert(db.dm.entity, psc, sql, map, holder)

        if (db.dm.id == null)
            db.dm.id = conversionService.convert(holder.keys!![mapping.idColumn],
                    mapping[mapping.idColumn!!].kotlinType.java)

        val guid = db.dm.newMapGuid!!
        db.dm.persisted()

        fc.newids[guid] = db.dm

        return Pair(sql, map)
    }

    private fun doInsert(entity: String, psc: PreparedStatementCreator, sql: String,
                         map: LinkedHashMap<String, Any?>, holder: GeneratedKeyHolder) {

        try {
            namedParameterJdbcTemplate.jdbcOperations.update(
                    PreparedStatementCreatorExtd(psc, sql, map),
                    holder)
        } catch (e: RuntimeException) {
            dbExceptionsMessageTranslator.makeMessageUserFriendlyOrRethrow(entity, e)
        }
    }


    private fun doUpdate(entity: String, sql: String, map: Map<String, Any?>) {
        try {
            namedParameterJdbcTemplate.update(sql, map)
        } catch (e: RuntimeException) {
            dbExceptionsMessageTranslator.makeMessageUserFriendlyOrRethrow(entity, e)
        }
    }

    private fun extractParam(newValue: Any?, mapping: DataMapping, delta: Delta, fc: FlushContext): Any? {
        return when {
            newValue != null && mapping[delta.property!!].isJson() -> dbDialect.getJsonParamHolderObject(newValue)
            newValue is DataMap -> getDataMapId(newValue, fc)
            newValue is ValuedEnum<*> -> newValue.value
            else -> newValue
        }
    }

    private fun getDataMapId(dm: DataMap, fc: FlushContext): Any? {
        if (dm.isNew()) {
            makeSure(fc.newids.containsKey(dm.newMapGuid))
            return fc.newids[dm.newMapGuid]?.id
        }
        if (dm.id is Map<*, *>)
            return AnyValue(dm.id).value()
        return dm.id
    }


    /**
     * 1) Для всех операций добавления сущности в список, мы проставляем реальную "обратную пропертю"
     * по которой и будет происходить вставка в базу.
     * 2) При этом, после того как мы проставили BackRefField ("обратное пропертю коллекции") мы должны
     * проверить не было ли явной вставки черех обратную пропертю в рамках этого же бакета
     * (реальный кейц)
     * */
    private fun updateBackRefsInBucket(b: DeltaBucket) {

        //1)
        val addToL = b.deltas.filter { it.type == DeltaType.ADD_TO_LIST }
                .map { delta ->
                    val property = dataMapsUtilService.getBackRefField(delta.parent!!, delta.parentProperty!!)
                    delta.property = property
                    delta.newValue = AnyValue(delta.parent)
                    delta
                }.toList()

        //2)
        addToL.forEach { addDelta ->
            if (b.deltas.count { it.property == addDelta.property } > 1) {
                val removeDelta = b.deltas.find { it.property == addDelta.property }
                b.deltas.remove(removeDelta)
            }
        }
    }


    fun updateBackRef(parent: DataMap, child: DataMap, property: String) {
        dataMapsUtilService.updateBackRef(parent, child, property, true)
    }

    private fun convertMapValuesForDb(map: MutableMap<String, Any?>): Map<String, Any?> {

        map.forEach { t, u ->
            val classToConvert = convertHelper.getJavaTypeToConvertToDb(u)
            classToConvert?.let {
                map[t] = conversionService.convert(u, it)
            }
        }
        return map
    }

}



