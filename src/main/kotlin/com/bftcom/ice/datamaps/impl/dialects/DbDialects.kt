package com.bftcom.ice.datamaps.impl.dialects

import com.bftcom.ice.datamaps.utils.AppException
import com.bftcom.ice.datamaps.utils.caseInsMapOf
import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.utils.GUID
import com.bftcom.ice.datamaps.impl.mappings.ForeignKeyCascade
import com.bftcom.ice.datamaps.impl.util.toJson
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.JDBCType
import kotlin.reflect.KClass

class HsqldbDialect(private val dbInfo: DbDialect.DbInfo) : DbDialect {

    override fun getDbInfo(): DbDialect.DbInfo = dbInfo

    override fun getDbIdentifier(name: String): String = name.toUpperCase()

    override fun getCurrentScheme(): String = "public".toUpperCase()

    override fun getLimitOffsetQueryInSelect(limit: Int?, offset: Int?): String {
        if (limit != null || offset != null) {
            return "LIMIT ${offset ?: 0} ${limit ?: 0} "
        }
        return " "
    }

    override fun ddlAddScalarColumn(table: String, columnName: String, jdbcType: JDBCType,
                                    length: Int?, notNull: Boolean): String {
        val ls = if (length != null && length > 0) "($length)" else ""
        val notnull = if (notNull) " NOT NULL" else ""
        return "ALTER TABLE $table ADD COLUMN \"$columnName\" ${getDbTypeBJDBCType(jdbcType)}$ls $notnull;\n"
    }

    override fun ddlDropColumn(table: String, name: String): String {
        return "ALTER TABLE $table DROP COLUMN IF EXISTS \"${name}\";\n"
    }

    fun getDbTypeBJDBCType(jdbcType: JDBCType, length: Int = 50): String {
        return when {
            jdbcType == String::class -> "varchar($length)"
            jdbcType == Int::class -> " integer "
            else -> TODO()
        }
    }

    override fun getJsonParamHolderObject(newValue: Any?): Any? {
        if (newValue is DataMap)
            return newValue.toJson(writeSystemProps = false)
        return newValue
    }
}

class FirebirdDialect(private val dbInfo: DbDialect.DbInfo, val jdbcTemplate: JdbcTemplate) : DbDialect {

    init {
        dbInfo.profile = "firebird"
    }

    override fun getDbIdentifier(name: String): String = name.toUpperCase()


    override fun readPrimaryKeySequenceName(c: Connection, md: DatabaseMetaData, catalog: String?, scheme: String?, table: String): String? {
        val name = getDbIdentifier(table + "_SEQ")

        var flag = false
        jdbcTemplate.query("select RDB\$GENERATOR_NAME FROM RDB\$GENERATORS WHERE RDB\$GENERATOR_NAME = '$name'") { _, _ ->
            run {
                flag = true
            }
        }
        return if (!flag) null else name
    }

    override fun getJsonParamHolderObject(newValue: Any?): Any? {
        if (newValue is DataMap)
            return newValue.toJson(writeSystemProps = false)
        return newValue
    }

    override fun getLimitOffsetQueryInSelect(limit: Int?, offset: Int?): String {
        val x = if (limit != null && limit > 0)
            " FIRST $limit"
        else ""

        val y = if (offset != null && offset > 0)
            " SKIP $offset"
        else ""
        return x + y
    }

    override fun getDbInfo() = dbInfo
}

class PostgresqlDialect(private val dbInfo: DbDialect.DbInfo, val jdbcTemplate: JdbcTemplate) : DbDialect {
    private val log = LoggerFactory.getLogger(this.javaClass)
    private val connection = jdbcTemplate.dataSource.connection

    private val keywords = caseInsMapOf<String>()

    init {
        dbInfo.profile = "postgresql"
        keywords["user"] = "user"
        keywords["data"] = "data"
        try {
            parseUrlToDbInfo(dbInfo, connection.metaData.url)
            dbInfo.schema = getCurrentScheme()
        }catch (e: RuntimeException) {
            log.error(e.message)
        }
    }

    override fun getDbInfo(): DbDialect.DbInfo = dbInfo

    override fun getDbIdentifier(name: String): String = name.toLowerCase()

    override fun getQuotedDbIdentifier(id: String?): String {
        val i = id!!
        if (keywords.containsKey(i))
            return "\"$i\""
        return i
    }

    override fun appendLimitOffset(sb: StringBuilder, limit: Int?, offset: Int?, params: MutableMap<String, Any?>) {
        var res = ""
        limit?.let {
            if (limit != 0) {
                res += " LIMIT :_limit"
                params["_limit"] = limit
            }

        }
        offset?.let {
            res += " OFFSET :_offset"
            params.put("_offset", offset)
        }
        sb.append(res + " ")

    }

    override fun castSqlExpressionToType(sqlExpression: String, type: JDBCType?): String {
        if (type == null)
            return sqlExpression

        return when (type) {
            JDBCType.INTEGER, JDBCType.BIGINT, JDBCType.DOUBLE -> "($sqlExpression)::numeric"
            JDBCType.ROWID -> "($sqlExpression)::uuid"
            JDBCType.DATE -> "($sqlExpression)::date"
            else -> sqlExpression
        }
    }

    override fun ddlCreateTableWithIdentity(table: String, idColumn: String, idType: KClass<*>): String {
        validateTableName(table)

        val idTypeString = when (idType) {
            GUID::class -> "UUID"
            else -> "BIGSERIAL"
        }

        return "CREATE TABLE IF NOT EXISTS $table (${System.lineSeparator()}" +
                "   $idColumn $idTypeString PRIMARY KEY${System.lineSeparator()}" +
                ");${System.lineSeparator()}"

    }

    private val tableNameRegex = Regex("^[A-Za-z][A-Za-z0-9_]{0,100}\$")
    private fun validateTableName(table:String) {
        if(!tableNameRegex.matches(table))
            throw AppException(message = "Наименование таблицы некорректно. Пожалуйста, исправьте")
    }

    override fun ddlAddScalarColumn(table: String, columnName: String, jdbcType: JDBCType, length: Int?, notNull: Boolean): String {
        val notnull = if (notNull) "NOT NULL" else ""
        val default = if (notNull) getDefaultValueForNotNullTypeByJdbcType(jdbcType, columnName) else ""
        return "ALTER TABLE $table ADD COLUMN IF NOT EXISTS $columnName " +
                "${getDbTypeByJdbcType(jdbcType, length)} $notnull$default;${System.lineSeparator()}"
    }

    override fun ddlAddReferenceColumn(table: String, columnName: String, jdbcType: JDBCType, fkTable: String, fkColumnName: String, cascade: ForeignKeyCascade, fkName: String): String {
        val fkNameL = fkName.toLowerCase()
        return "ALTER TABLE $table ADD COLUMN IF NOT EXISTS ${columnName} ${getDbTypeByJdbcType(jdbcType)};${System.lineSeparator()}" +
                "ALTER TABLE $table DROP CONSTRAINT  IF EXISTS   $fkNameL;${System.lineSeparator()}" +
                "ALTER TABLE $table ADD CONSTRAINT $fkNameL FOREIGN KEY ($columnName) REFERENCES $fkTable($fkColumnName)" +
                when (cascade) {
                    ForeignKeyCascade.Cascade -> " ON DELETE CASCADE"
                    else -> ""
                } +
                ";${System.lineSeparator()}"

    }

    override fun ddlDropColumn(table: String, name: String): String {
        return "ALTER TABLE $table DROP COLUMN IF EXISTS \"${name}\" CASCADE;\n"
    }

    override fun getJsonParamHolderObject(newValue: Any?): Any? {
        val strValue: String? = when (newValue) {
            is DataMap -> newValue.toJson(writeSystemProps = false)
            null -> null
            else -> newValue.toString()
        }
        return PGobject().apply { this.type = "jsonb"; value = strValue }
    }

    companion object {


        fun getDbTypeByJdbcType(type: JDBCType, length: Int? = 0): String {

            return when (type) {
                JDBCType.VARCHAR -> {
                    if (length != null && length > 0)
                        "character varying($length)"
                    else "text"
                }
                JDBCType.INTEGER -> "integer"
                JDBCType.BIGINT -> "integer"
                JDBCType.BOOLEAN -> "boolean"
                JDBCType.DATE -> "date"
                JDBCType.TIMESTAMP -> "timestamp"
                JDBCType.STRUCT -> "jsonb"
                JDBCType.ROWID -> "UUID"
                JDBCType.DOUBLE -> "decimal"
                else -> TODO()
            }
        }


        private fun getDefaultValueForNotNullTypeByJdbcType(type: JDBCType, columnName: String): String {

            return when {
                type == JDBCType.VARCHAR -> " DEFAULT '???' "
                type == JDBCType.INTEGER -> " DEFAULT 0 "
                type == JDBCType.DECIMAL -> "  DEFAULT 0 "
                type == JDBCType.BOOLEAN -> " DEFAULT FALSE "
                type == JDBCType.TIMESTAMP -> " DEFAULT Now() "
                type == JDBCType.DATE && columnName.toLowerCase().contains("start") -> " DEFAULT '-infinity' "
                type == JDBCType.DATE && columnName.toLowerCase().contains("end") -> " DEFAULT 'infinity' "
                else -> ""
            }
        }
    }
}

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