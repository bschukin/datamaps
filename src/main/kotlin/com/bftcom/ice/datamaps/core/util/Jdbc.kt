package com.bftcom.ice.datamaps.core.util

import com.bftcom.ice.datamaps.misc.FieldType
import com.bftcom.ice.datamaps.misc.NIY
import com.bftcom.ice.datamaps.DataMapF
import com.bftcom.ice.datamaps.misc.Date
import com.bftcom.ice.datamaps.misc.GUID
import com.bftcom.ice.datamaps.misc.Timestamp
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.support.KeyHolder
import java.sql.JDBCType
import kotlin.reflect.KClass

//https://db.apache.org/ojb/docu/guides/jdbc-types.html
fun getJavaTypeByJDBCType(jdbcType: JDBCType, size: Int = 10, decimalDigits: Int = 0): KClass<*> {
    return when (jdbcType) {
        JDBCType.CHAR, JDBCType.VARCHAR, JDBCType.LONGNVARCHAR -> String::class
        JDBCType.NUMERIC, JDBCType.DECIMAL -> {
            if (size == 1 && decimalDigits == 0)
                return Boolean::class
            if (decimalDigits > 0)
                return Double::class
            return Int::class
        }
        JDBCType.INTEGER -> Int::class
        JDBCType.BIT, JDBCType.BOOLEAN -> Boolean::class
        JDBCType.TINYINT -> Byte::class
        JDBCType.SMALLINT -> Short::class
        JDBCType.BIGINT -> Long::class
        JDBCType.REAL -> Double::class
        JDBCType.FLOAT -> Double::class
        JDBCType.DOUBLE -> Double::class
        JDBCType.DATE -> Date::class
        JDBCType.TIME -> java.sql.Time::class
        JDBCType.TIMESTAMP -> Timestamp::class
        JDBCType.CLOB, JDBCType.LONGVARCHAR -> String::class
        JDBCType.BINARY, JDBCType.VARBINARY, JDBCType.LONGVARBINARY -> ByteArray::class
        JDBCType.BLOB -> ByteArray::class
        JDBCType.STRUCT -> DataMapF::class
        JDBCType.ARRAY -> List::class
        JDBCType.DISTINCT, JDBCType.DATALINK, JDBCType.REF -> throw NIY()
        JDBCType.ROWID -> GUID::class
        else -> throw NIY()
    }
}

fun FieldType.getJDBCTypeByFieldType(): JDBCType {
    return when (this) {
        FieldType.Bool -> JDBCType.BOOLEAN
        FieldType.String -> JDBCType.VARCHAR
        FieldType.Enum -> JDBCType.VARCHAR
        FieldType.Int -> JDBCType.INTEGER
        FieldType.Long -> JDBCType.BIGINT
        FieldType.Double -> JDBCType.DOUBLE
        FieldType.Date -> JDBCType.DATE
        FieldType.Timestamp -> JDBCType.TIMESTAMP
        FieldType.ByteArray -> JDBCType.BLOB
        FieldType.Json -> JDBCType.STRUCT
        FieldType.Clob -> JDBCType.CLOB
        FieldType.Guid -> JDBCType.ROWID
        FieldType.Array -> TODO()
        FieldType.Other -> JDBCType.OTHER
    }
}

fun getJDBCTypeByJdbcMetadata(jdbcType: JDBCType, typeName: String?): JDBCType {

    return when (typeName != null) {
        "JSONB".equals(typeName, true) &&
                jdbcType == JDBCType.OTHER -> JDBCType.STRUCT
        "UUID".equals(typeName, true) -> JDBCType.ROWID
        else -> jdbcType
    }
}

class NamedJdbcTemplateWrapper(private val template: NamedParameterJdbcTemplate,
                               private val sqlStatistics: SqlStatistics)
    : NamedParameterJdbcOperations by template {


    @Throws(DataAccessException::class)
    override fun <T> query(sql: String, paramMap: Map<String, *>, rowMapper: RowMapper<T>): List<T> {
        val start = System.currentTimeMillis()
        val res = template.query(sql, paramMap, rowMapper)
        val end = System.currentTimeMillis()

        sqlStatistics.addSqlStat(sql, paramMap, end - start)

        return res

    }

    @Throws(DataAccessException::class)
    override fun update(sql: String, paramMap: Map<String, *>): Int {
        val start = System.currentTimeMillis()
        val res = template.update(sql, paramMap)
        val end = System.currentTimeMillis()

        sqlStatistics.addSqlUpdate(sql, paramMap, end - start)

        return res
    }
}


class JdbcTemplateWrapper(private val template: JdbcTemplate,
                          private val sqlStatistics: SqlStatistics)
    : JdbcOperations by template {

    @Throws(DataAccessException::class)
    override fun update(psc: PreparedStatementCreator, generatedKeyHolder: KeyHolder): Int {
        val start = System.currentTimeMillis()
        val res = template.update(psc, generatedKeyHolder)

        val end = System.currentTimeMillis()
        if (psc is PreparedStatementCreatorExtd)
            sqlStatistics.addSqlUpdate(psc.sql, psc.map, end - start)

        return res
    }
}


data class PreparedStatementCreatorExtd(val preparedStatementCreator: PreparedStatementCreator,
                                        val sql: String,
                                        val map: Map<String, *>) : PreparedStatementCreator by preparedStatementCreator