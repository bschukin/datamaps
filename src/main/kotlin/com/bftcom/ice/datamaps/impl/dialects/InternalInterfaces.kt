package com.bftcom.ice.datamaps.impl.dialects

import com.bftcom.ice.datamaps.utils.throwNotImplementedYet
import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.DataService
import com.bftcom.ice.datamaps.impl.mappings.ForeignKeyCascade
import com.bftcom.ice.datamaps.utils.GUID
import com.bftcom.ice.datamaps.impl.mappings.DataMapping
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.JDBCType
import java.util.*
import java.util.stream.Stream
import kotlin.reflect.KClass

/**
 * Created by Щукин on 27.10.2017.
 */

interface DataServiceExtd : DataService {
    fun getDataMapping(name: String): DataMapping

    fun deleteAll(entity: String)

    /**
     * Быстрая массовая вставка записей в базу.
     * Айдишник должен быть либо уже в датамапе, либо присваиваться при инсерте как identity (serial)
     */
    fun bulkInsert(list: List<DataMap>, runBeingOperations: Boolean = true, presInsertAction: ((DataMap) -> Unit)? = null)

    /**
     * Быстрая массовая вставка записей в базу.
     * Айдишник должен быть либо уже в датамапе, либо присваиваться при инсерте как identity (serial)
     */
    fun bulkInsert(stream: Stream<DataMap>, entity: String, runBeingOperations: Boolean = true, presInsertAction: ((DataMap) -> Unit)? = null)
}

/**
 * Created by b.schukin on 23.11.2017.
 */
interface DbDialect {
    data class DbVersion(val major: Int, val minor: Int)

    data class DbInfo(val name: String, val version: DbVersion, var db: String?, var host: String?, var port: String?, var schema: String?, var profile: String?=null)

    fun getDbInfo(): DbInfo

    fun getDbIdentifier(name: String): String = name

    fun getCurrentScheme(): String? = "public"

    //читаем сиквенс первичного ключа
    //он равен {$table_SEQ}. если есть - будем использовать сиквенс для генерации первичного ключа
    fun readPrimaryKeySequenceName(c: Connection, md: DatabaseMetaData, catalog: String?, scheme: String?, table: String):String? {
        val rs2 = md.getTables(catalog, scheme,
                getDbIdentifier(table + "_SEQ"), arrayOf("SEQUENCE"))
        var res:String? = null
        rs2.use {
            while (rs2.next())
                res= rs2.getString("TABLE_NAME")
        }
        return res
    }

    fun getLimitOffsetQueryInSelect(limit: Int?, offset: Int?): String = ""
    fun appendLimitOffset(sb: StringBuilder, limit: Int?, offset: Int?, params: MutableMap<String, Any?>) {}

    fun getQuotedDbIdentifier(id: String?): String = "\"$id\""

    fun ddlCreateTableWithIdentity(table: String, idColumn: String, idType: KClass<*>): String = TODO()

    fun ddlAddScalarColumn(table: String, columnName: String, jdbcType: JDBCType, length: Int? = 0, notNull: Boolean): String = TODO()

    fun ddlAddReferenceColumn(table: String, columnName: String, jdbcType: JDBCType,
                              fkTable: String, fkColumnName: String, cascade: ForeignKeyCascade = ForeignKeyCascade.None,
                              fkName: String = table + "_" + columnName + "_FK"): String {
        TODO()
    }

    fun ddlDropColumn(table: String, name: String): String = TODO()

    fun isIdentityColumn(table: String, column: String): Boolean = false
    fun getJsonParamHolderObject(newValue: Any?): Any? = TODO()

    fun castSqlExpressionToType(sqlExpression: String, type: JDBCType?): String = sqlExpression

    fun castSqlExpressionToType(sqlExpression: String, valueOfTargetType: Any?): String {
        return when (valueOfTargetType) {
            is Number -> castSqlExpressionToType(sqlExpression, JDBCType.INTEGER)
            is UUID -> castSqlExpressionToType(sqlExpression, JDBCType.ROWID)
            is GUID -> castSqlExpressionToType(sqlExpression, JDBCType.ROWID)
            else -> sqlExpression
        }
    }
}


fun getDbDialectByConnection(jdbcTemplate: JdbcTemplate): DbDialect {
    val c = jdbcTemplate.dataSource.connection
    c.use {
        val dbVersion = DbDialect.DbVersion(c.metaData.databaseMajorVersion,
                c.metaData.databaseMinorVersion)
        val v = DbDialect.DbInfo(c.metaData.databaseProductName, dbVersion,
                null, null, null, null)
        return when (c.metaData.databaseProductName.toLowerCase()) {
            "PostgreSQL".toLowerCase() -> PostgresqlDialect(v, jdbcTemplate)
            "HSQL Database Engine".toLowerCase() -> HsqldbDialect(v)
            "Oracle".toLowerCase() -> OracleDialect(v, jdbcTemplate)
            "Firebird 2.5".toLowerCase()-> FirebirdDialect(v,jdbcTemplate)
            "Firebird 3.0".toLowerCase()-> FirebirdDialect(v,jdbcTemplate)
            else -> throwNotImplementedYet("unknown dialect: ${c.metaData.databaseProductName}")
        }
    }
}
