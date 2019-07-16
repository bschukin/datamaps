package com.bftcom.ice.server.datamaps

import com.bftcom.ice.common.maps.DataMap
import com.bftcom.ice.server.datamaps.mappings.DataField
import com.bftcom.ice.server.datamaps.mappings.DataMapping
import com.bftcom.ice.server.datamaps.mappings.DataMappingsService
import com.bftcom.ice.server.datamaps.mappings.IdGenerationType
import com.bftcom.ice.server.services.DbDialect
import com.bftcom.ice.server.util.toJson
import org.postgresql.copy.CopyManager
import org.postgresql.jdbc.PgConnection
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DataSourceUtils
import org.springframework.stereotype.Service
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors.groupingBy
import java.util.stream.Stream


interface BulkInserter {
    /***
     * Вставка большого количества записей одной(!) сущности
     */
    fun bulkInsert(list: List<DataMap>, runBeingOperations: Boolean = true, presInsertAction: ((DataMap) -> Unit)? = null) {
        TODO()
    }

    fun bulkInsert(stream: Stream<DataMap>, entity: String, runBeingOperations: Boolean = true, presInsertAction: ((DataMap) -> Unit)? = null) {
        TODO()
    }
}

@Service("bulkInserter")
@Profile("oracle", "hsqldb", "firebird")
class DefaultBulkInserter : BulkInserter

@Service("bulkInserter")
@Profile("postgresql")
class PostgessBulkInserter : BulkInserter {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(PostgessBulkInserter::class.java)
    }

    @Autowired
    private lateinit var dataMappingsService: DataMappingsService

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var dataMapTriggersRegistry: DataMapTriggersRegistry

    @Autowired
    private lateinit var dbDialect: DbDialect


    override fun bulkInsert(list: List<DataMap>, runBeingOperations: Boolean, presInsertAction: ((DataMap) -> Unit)?) {
        if (list.isEmpty())
            return

        if (runBeingOperations)
            dataMapTriggersRegistry.runInsertTriggers(list)

        if (presInsertAction != null)
            list.forEach {
                presInsertAction(it)
            }

        doCopyCommandWithTimeMachine(list, list.first().entity)
    }


    override fun bulkInsert(stream: Stream<DataMap>,entity: String, runBeingOperations: Boolean, presInsertAction: ((DataMap) -> Unit)?) {

        val counter = AtomicInteger()
        val chunkSize = 10000

        val grouping = { x: DataMap -> counter.getAndIncrement() / chunkSize }

        if (runBeingOperations)
            dataMapTriggersRegistry
                    .runInsertTriggers(stream, presInsertAction)
                    .collect(groupingBy(grouping))
                    .values.forEach { chunk ->
                doCopyCommandWithTimeMachine(chunk, entity)
            }
        else
            stream.collect(groupingBy(grouping))
                    .values.forEach { chunk ->
                doCopyCommandWithTimeMachine(chunk ,entity)
            }
    }

    private fun doCopyCommand(list: List<DataMap>, entity: String) {
        val start = System.currentTimeMillis()
        //маппинг берется из датамап, потому что ентити может быть именем временной таблицы, у которой нет маппинга
        val mapping = dataMappingsService.getDataMapping(list.first().entity)
        val fields = getFieldNamesToInsert(mapping)
        val header =
                "COPY ${entity} (" +
                        fields.map {
                            dbDialect.getQuotedDbIdentifier(it.sqlcolumn)
                        }.joinToString() + ") FROM STDIN WITH DELIMITER '|' NULL 'NULL' "

        val copyManager = obtainCopyManager()

        val copyIn = copyManager.copyIn(header)

        try {
            list.forEach {
                val rowString = buildRow(it, fields)
                val bytes = rowString.toByteArray()
                copyIn.writeToCopy(bytes, 0, bytes.size)
            }
            copyIn.endCopy()
        } finally {
            if (copyIn.isActive) {
                copyIn.cancelCopy()
            }
        }

        val end = System.currentTimeMillis()
        LOGGER.trace("bulk insert of ${list.size} records took " + (end - start).toDouble() / 1000.0+ " ms")
    }

    private fun doCopyCommandWithTimeMachine(list: List<DataMap>, entity: String) {
        doCopyCommand(list, entity)

        // TODO ICE-440
        /*timeMachine?.let {
            doCopyCommand(it.bulkInsert(list), entity)
        }*/
    }

    private fun obtainCopyManager(): CopyManager {
        val c = DataSourceUtils.getConnection(jdbcTemplate.dataSource)
        val copyOperationConnection = c.unwrap(PgConnection::class.java)
        return CopyManager(copyOperationConnection)
    }


    private fun buildRow(dm: DataMap, fields: List<DataField>): String {

        fun escape(value: Any): String {
            return when (value) {
                is DataMap -> value.toJson(false)
                else -> value.toString()
            }
             .replace("\n", "")
             .replace("\\n", "")
             .replace("\\t", "   ")
             .replace("\\\\", "\\\\\\\\")
             .replace("\\\"", "\\\\\"")
             .replace("|", "~")
        }

        fun getFormattedValue(df: DataField, value: Any?): String {
            return when {
                value == null -> "NULL"
                df.isJson() -> escape(value)
                df.kotlinType ==String::class  -> escape(value)
                df.isM1()-> (value as? DataMap)?.id?.toString()?:"NULL"
                else -> value.toString()
            }
        }

        return fields.map { getFormattedValue(it, dm[it.name]) }.joinToString("|")
                .plus("\n")

    }

    private fun getFieldNamesToInsert(mapping: DataMapping): List<DataField> {
        val list = mutableListOf<DataField>()
        mapping.fields.forEach { t, u ->
            if (u.equals(mapping.idField())) {
                if (mapping.idGenerationType == IdGenerationType.NONE)
                    list.add(u)
            } else if (u.isSimple() || u.isM1())
                list.add(u)
            else if(!u.is1N())
                TODO()
        }
        return list
    }


}
