package com.bftcom.ice.datamaps.impl.dialects

import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.impl.util.toJson
import java.sql.JDBCType

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