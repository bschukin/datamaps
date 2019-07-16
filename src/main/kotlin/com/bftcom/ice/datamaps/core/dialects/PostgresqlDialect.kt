package com.bftcom.ice.datamaps.core.dialects

import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.core.mappings.ForeignKeyCascade
import com.bftcom.ice.datamaps.core.util.toJson
import com.bftcom.ice.datamaps.misc.AppException
import com.bftcom.ice.datamaps.misc.GUID
import com.bftcom.ice.datamaps.misc.caseInsMapOf
import org.postgresql.util.PGobject
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.JDBCType
import kotlin.reflect.KClass

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