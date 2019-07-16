package com.bftcom.ice.datamaps.impl.dialects

import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.impl.util.toJson
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate

class OracleDialect(private val dbInfo: DbDialect.DbInfo, val jdbcTemplate: JdbcTemplate) : DbDialect {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val connection = jdbcTemplate.dataSource.connection

    init {
        try {
            parseUrlToDbInfo(dbInfo, connection.metaData.url)
            dbInfo.schema = getCurrentScheme()
        }catch (e: RuntimeException) {
            log.error(e.message)
        }
    }

    override fun getDbInfo(): DbDialect.DbInfo = dbInfo

    override fun getCurrentScheme(): String? {
        return connection.metaData.userName
    }

    override fun getDbIdentifier(name: String): String = name.toUpperCase()

    /****
     * -- Oracle 11g and less
    SELECT *
    FROM (
    SELECT b.*, ROWNUM RN
    FROM (
    SELECT *
    FROM BOOK
    ORDER BY ID ASC
    ) b
    WHERE ROWNUM <= 3
    )
    WHERE RN > 2
     */
    override fun appendLimitOffset(sb: StringBuilder, limit: Int?, offset: Int?, params: MutableMap<String, Any?>) {
        val top = if (limit == null) 0 else limit + if (offset == null) 0 else offset
        limit?.let {
            if (limit != 0) {
                sb.insert(0, "SELECT bbb__.*, ROWNUM RN__ FROM (")
                sb.append(" ) bbb__ WHERE ROWNUM <= :_top")
                params.put("_top", top)
            }
        }
        offset?.let {
            if (limit == null || limit == 0) {
                sb.insert(0, "SELECT * FROM (SELECT bbb__.*, ROWNUM RN__ FROM (")
                sb.append(" ) bbb__ ) WHERE RN__ > :_offset")
            } else {
                sb.insert(0, "SELECT * FROM (")
                sb.append(") WHERE RN__ > :_offset")
            }

            params.put("_offset", offset)
        }
    }

    override fun isIdentityColumn(table: String, column: String): Boolean {
        val sql = "SELECT COUNT(*) FROM ALL_TRIGGERS\n" +
                "WHERE TABLE_NAME = '$table' and TRIGGER_TYPE = 'BEFORE EACH ROW'\n" +
                "and TRIGGERING_EVENT = 'INSERT'"

        val count = jdbcTemplate.queryForObject(sql, Integer::class.java)
        return count > 0
    }

    override fun getJsonParamHolderObject(newValue: Any?): Any? {
        if (newValue is DataMap)
            return newValue.toJson(writeSystemProps = false)
        return newValue
    }
}

fun parseUrlToDbInfo(dbInfo: DbDialect.DbInfo, jdbcURL: String) {

    var host: String? = null
    var port: String? = null
    var db: String? = null

    var workUrl  = ""

    try {
        if (jdbcURL.indexOf("@") == -1) {
            workUrl = jdbcURL.substring(jdbcURL.indexOf("://") + 3)
        } else {
            workUrl = jdbcURL.substring(jdbcURL.indexOf("@") + 1)
        }

        when (workUrl.split(":").size) {
            1 -> host = workUrl.split(":")[0]
            2 -> {
                host = workUrl.split(":")[0];
                if (workUrl.split(":")[1].indexOf("/") == -1)
                    port = workUrl.split(":")[1]
                else {
                    val lastPart = workUrl.split(":")[1]
                    port = lastPart.split("/")[0]
                    db = lastPart.split("/")[1]
                }
            }
            3 -> {
                host = workUrl.split(":")[0]; port = workUrl.split(":")[1]; db = workUrl.split(":")[2]
            }
            else -> host = jdbcURL
        }

        dbInfo.host = host
        dbInfo.port = port
        dbInfo.db = db
    } catch (e: Exception) {
        throw RuntimeException("Error parse JDBC url for dbInfo")
    }
}