package com.bftcom.ice.datamaps.impl.mappings

import com.bftcom.ice.datamaps.impl.dialects.DbDialect
import com.bftcom.ice.datamaps.impl.util.CacheClearable
import com.bftcom.ice.datamaps.impl.util.getJDBCTypeByJdbcMetadata
import com.bftcom.ice.datamaps.utils.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.JDBCType
import java.sql.ResultSet
import java.util.stream.Collectors
import kotlin.streams.toList

/**
 * Created by Щукин on 03.11.2017.
 *
 * Метаданные таблиц БД (обертка над JDBC-метаданными базы)
 */

interface DbMetadataService {

    /**
     * Получить инфу о таблице базы данных (через JdbcMetadata)*/
    fun getTableInfo(table: String): DbTable

    fun getTableInfo(table: String, throwIfNotFound: Boolean = true): DbTable?

    /**
     * очистить инфу о таблице базы данных (через JdbcMetadata)*/
    fun clearTableInfo(table: String): DbTable?

    fun getExportedKeys(): List<ForeignKey>

    fun getImportedKeys(table: String): List<ForeignKey>

    fun getUniqueIndexes():List<UniqueIndex>
}

class DbTable(val name: String,
              val primaryKeyField: String,
              val comment: String?) {

    //колонки которые реально присуствуют в таблице
    var columns = linkedCaseInsMapOf<DbColumn>()

    //колонки - не ссылки b yt
    val simpleColumns: List<DbColumn>
        get() = columns.values.stream().filter { t -> t.isSimple() }.toList()

    //колонки - ссылки
    val m1Columns: List<DbColumn>
        get() = columns.values.stream().filter { t -> t.isManyToOne() }.toList()

    //все экспортированные в другие колонки ключи
    var exportedKeys = mutableListOf<ForeignKey>()

    val oneToManyCollections: List<ForeignKey>
        get() = exportedKeys.stream().filter { t -> t.onDelete == ForeignKeyCascade.Cascade }.toList()

    var identitySequnceName: String? = null

    private val _uniqueIndexes = caseInsMapOf<UniqueIndex>()

    val uniqueIndexes: List<UniqueIndex> get() = _uniqueIndexes.values.filterNotNull().toList()

    operator fun get(column: String): DbColumn {
        if (!columns.containsKey(column))
            throw RuntimeException()
        return columns[column]!!
    }

    fun addColumn(value: DbColumn) {
        columns[value.name] = value
    }

    fun addExprotedKey(value: ForeignKey) {
        exportedKeys.add(value)
    }

    fun addUniqueIndex(name: String, column:String) {
        if(!_uniqueIndexes.containsKey(name))
            _uniqueIndexes[name] = UniqueIndex(this.name, name)
        _uniqueIndexes[name]!!.addColumn(column)
    }

    override fun toString(): String {
        return "DbTable(name='$name', primaryKeyField=$primaryKeyField, columns=$columns)"
    }


}

class DbColumn(val name: String, val jdbcType: JDBCType) {
    var ordinalPosition: Int = -1
    var size = 0
    var decimalDigits = 0
    var comment: String? = ""
    var isNullable = false
    var importedKey: ForeignKey? = null
    var isAutoIncrement = false

    fun isSimple(): Boolean {
        return importedKey == null
    }

    fun isManyToOne(): Boolean {
        return importedKey != null
    }

    fun isBackReference(): Boolean {
        return importedKey != null && importedKey!!.onDelete == ForeignKeyCascade.Cascade
    }

    fun isBlob(): Boolean {
        return jdbcType in blobs
    }

    fun isJson(): Boolean {
        return jdbcType == JDBCType.STRUCT
    }


    override fun toString(): String {
        return "DbColumn(n='$name', jdbcType=$jdbcType, comment=$comment, importedKey=$importedKey)"
    }

    companion object {
        val blobs = arrayOf(JDBCType.BLOB, JDBCType.CLOB, JDBCType.BINARY, JDBCType.VARBINARY, JDBCType.LONGVARBINARY)
    }
}

data class UniqueIndex(val table:String, val name: String) {
    private val cols = mutableListOf<String>()

    internal fun addColumn(name: String) {
        cols.add(name)
    }

    val columns: List<String> get() = cols

    override fun toString(): String {
        return "UniqueIndex(name='$name', cols=$cols)"
    }

}

class ForeignKey {
    var name: String = ""
    var pkTable: String = ""
    var pkColumn: String = ""
    var fkTable: String = ""
    var fkColumn: String = ""
    var onUpdate: ForeignKeyCascade = ForeignKeyCascade.None
    var onDelete: ForeignKeyCascade = ForeignKeyCascade.None
    override fun toString(): String {
        return "ForeignKey(name='$name', pkTable='$pkTable', pkColumn='$pkColumn', fkTableName='$fkTable', " +
                "fkColumnName='$fkColumn', onUpdate=$onUpdate, onDelete=$onDelete)"
    }

}

enum class ForeignKeyCascade(val value: Int) {
    Cascade(0),
    SetDefault(1),
    SetNull(2),
    None(3)
}


@Component
class GenericDbMetadataService : DbMetadataService, CacheClearable {

    @Autowired
    private lateinit var dbDialect: DbDialect

    @Autowired
    private lateinit var env: Environment

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private val tables = caseInsMapOf<DbTable>()

    fun isPostgreSql() = env.activeProfiles.contains("postgresql")
    fun isHsqlDb(): Boolean = env.activeProfiles.contains("hsqldb")
    fun isOracle(): Boolean = env.activeProfiles.contains("oracle")
    fun isFirebird(): Boolean = env.activeProfiles.contains("firebird")

    override fun clearCache() {
        tables.clear()
    }

    override fun getUniqueIndexes(): List<UniqueIndex> =
            tables.flatMap { it.value!!.uniqueIndexes }


    override fun clearTableInfo(table: String): DbTable? {
        return tables.remove(table)
    }


    override fun getTableInfo(table: String): DbTable {
        return getTableInfo(table, false)!!
    }

    override fun getTableInfo(table: String, throwIfNotFound: Boolean): DbTable? {
        if (tables.contains(table))
            return tables[table]!!

        val dbtable = readDbMetadata(table, throwIfNotFound) ?: return null
        tables[table] = dbtable

        return dbtable
    }

    override fun getImportedKeys(table: String): List<ForeignKey> {

        return getTableInfo(table).columns
                .values.stream()
                .filter { c -> c.importedKey != null }
                .map { c -> c.importedKey }
                .collect(Collectors.toList<ForeignKey>())
    }

    override fun getExportedKeys(): List<ForeignKey> {
        return tables.flatMap { it.value!!.exportedKeys }
    }


    private fun readDbMetadata(table: String, throwIfNotFound: Boolean = true): DbTable? {

        val c = jdbcTemplate.dataSource.connection
        //jdbcTemplate.dataSource.connection
        c.use {
            return readTableMetadata(c, table, throwIfNotFound)
        }
    }

    fun readTableMetadata(c: Connection, table: String, throwIfNotFound: Boolean = true): DbTable? {
        val md = c.metaData

        val rs = md.getTables(null, null, dbDialect.getDbIdentifier(table), null)
        rs.use {
            rs.next()
            if (!(rs.isFirst/* && rs.isLast*/)) {
                if (throwIfNotFound)
                    throw RuntimeException("Table '$table' not found in db")
                return null
            }

            val isView = "VIEW".equals(rs.getString("TABLE_TYPE"))
            val tableName = rs.getString("TABLE_NAME")
            val scheme = rs.getString("TABLE_SCHEM")
            val catalog = rs.getString("TABLE_CAT")

            val pkField = obtainPkColumnName(isView, md, table, scheme, catalog)

            //читаем таблицу
            val dt = DbTable(tableName, pkField, rs.getString("remarks")) //TODO: Для оракла надо комменты по другому собирать. см. testGetTableInfo

            //читаем колонки
            var crs = md.getColumns(catalog, scheme, dbDialect.getDbIdentifier(table), null)
            crs.use {
                while (crs.next()) {

                    val size = crs.getInt("COLUMN_SIZE")
                    val decimalDigits = crs.getInt("DECIMAL_DIGITS")
                    val column = DbColumn(crs.getString("COLUMN_NAME"),
                            getJDBCTypeByJdbcMetadata(
                                    JDBCType.valueOf(crs.getInt("DATA_TYPE")),
                                    crs.getString("TYPE_NAME")))

                    column.size = size
                    column.decimalDigits = decimalDigits
                    column.ordinalPosition = crs.getInt("ORDINAL_POSITION")
                    column.isNullable = crs.getBoolean("NULLABLE")
                    column.comment = crs.getString("REMARKS")
                    column.isAutoIncrement = getIsAutoIncrement(crs) ||
                            (column.name.equals(pkField, true) && dbDialect.isIdentityColumn(dt.name, column.name))
                    dt.addColumn(column)
                }
            }

            //читаем внешние ключи, на которые мы ссылаемся
            crs = md.getImportedKeys(catalog, scheme, dbDialect.getDbIdentifier(table))
            crs.use {
                while (crs.next()) {
                    val fk = ForeignKey()
                    fk.name = crs.getString("FK_NAME")
                    fk.pkTable = crs.getString("PKTABLE_NAME")
                    fk.pkColumn = crs.getString("PKCOLUMN_NAME")
                    fk.fkTable = crs.getString("FKTABLE_NAME")
                    fk.fkColumn = crs.getString("FKCOLUMN_NAME")
                    fk.onDelete = ForeignKeyCascade.values()[crs.getInt("DELETE_RULE")]
                    fk.onUpdate = ForeignKeyCascade.values()[crs.getInt("UPDATE_RULE")]

                    makeSure(fk.fkTable == dt.name)
                    dt[fk.fkColumn].importedKey = fk
                }
            }

            //читаем связи, которые  мы экспортировали в другие таблицы -
            //и решаем - является ли связь коллекцией
            //определеяем это по каскаду
            crs = md.getExportedKeys(catalog, scheme, dbDialect.getDbIdentifier(table))
            crs.use {
                while (crs.next()) {
                    val fk = ForeignKey()
                    fk.name = crs.getString("FK_NAME")
                    fk.pkTable = crs.getString("PKTABLE_NAME")
                    fk.pkColumn = crs.getString("PKCOLUMN_NAME")
                    fk.fkTable = crs.getString("FKTABLE_NAME")
                    fk.fkColumn = crs.getString("FKCOLUMN_NAME")
                    fk.onDelete = ForeignKeyCascade.values()[crs.getInt("DELETE_RULE")]
                    fk.onUpdate = ForeignKeyCascade.values()[crs.getInt("UPDATE_RULE")]

                    dt.addExprotedKey(fk)
                }
            }

             //читаем уникальные индексы
            crs = md.getIndexInfo(catalog, scheme, dbDialect.getDbIdentifier(table), true, true)
            crs.use {
                while (crs.next()) {
                    val indexName = crs.getString("INDEX_NAME")
                    val columnName = crs.getString("COLUMN_NAME")
                    indexName?.let{
                        dt.addUniqueIndex(indexName, columnName)
                    }
                }
            }

            //читаем сиквенс первичного ключа
            //он равен {$table_SEQ}. если есть - будем использовать сиквенс для генерации первичного ключа
            dt.identitySequnceName = dbDialect.readPrimaryKeySequenceName(c, md, catalog, scheme, table)

            return dt
        }
    }

    private fun obtainPkColumnName(isView: Boolean, md: DatabaseMetaData, table: String, scheme: String?, catalog: String?): String {
        when (isView) {
            true -> {
                val crs = md.getColumns(catalog, scheme,
                        dbDialect.getDbIdentifier(table), null)
                crs.next()
                return crs.getString("COLUMN_NAME")
            }

            else -> {
                var pkCol: String? = null
                val pk = md.getPrimaryKeys(catalog, scheme, dbDialect.getDbIdentifier(table))
                while (pk.next()) {
                    val columnName = pk.getString("COLUMN_NAME")
                    if (pkCol != null && columnName != pkCol) throw NIY("primary key with multiple columns is not supported")
                    pkCol = columnName
                }
                return pkCol!!
            }
        }
    }

    private fun getIsAutoIncrement(crs: ResultSet): Boolean {
        val obj = crs.getObject("IS_AUTOINCREMENT")
        return when (obj) {
            is String -> {
                "YES".equals(obj, true)
            }
            is Boolean -> obj
            else -> throwImpossible()
        }
    }

}

