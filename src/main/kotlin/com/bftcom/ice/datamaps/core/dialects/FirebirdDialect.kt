package com.bftcom.ice.datamaps.core.dialects

import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.core.util.toJson
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Connection
import java.sql.DatabaseMetaData

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