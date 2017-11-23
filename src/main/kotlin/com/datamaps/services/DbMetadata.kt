package com.datamaps.services

import com.datamaps.general.NIY
import com.datamaps.util.caseInsMapOf
import com.datamaps.util.linkedCaseInsMapOf
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import java.sql.JDBCType
import java.util.stream.Collectors
import javax.annotation.Resource
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

    fun getImportedKeys(table: String): List<ForeignKey>
}

class DbTable(val name: String, val primaryKeyField: String?, val comment: String?) {

    //колонки которые реально присуствуют в таблице
    var columns = linkedCaseInsMapOf<DbColumn>()

    //колонки - не ссылки
    val simpleColumns: List<DbColumn>
        get() = columns.values.stream().filter { t -> t.isSimple() }.toList()

    //колонки - ссылки
    val m1Columns: List<DbColumn>
        get() = columns.values.stream().filter { t -> t.isManyToOne() }.toList()

    //все экспортированные в другие колонки ключи
    var exportedKeys = mutableListOf<ForeignKey>()

    val oneToManyCollections: List<ForeignKey>
        get() = exportedKeys.stream().filter { t -> t.onDelete == ForeignKeyCascade.cascade }.toList()

    operator fun get(column: String): DbColumn {
        if (!columns.containsKey(column))
            throw RuntimeException()
        return columns.get(column)!!
    }

    fun addColumn(value: DbColumn) {
        columns[value.name] = value
    }

    fun addExprotedKey(value: ForeignKey) {
        exportedKeys.add(value)
    }

    override fun toString(): String {
        return "DbTable(name='$name', primaryKeyField=$primaryKeyField, columns=$columns)"
    }


}

class DbColumn(val name: String, val jdbcType: JDBCType) {
    var ordinalPosition: Int = -1
    var size: Int = 0
    var comment: String? = ""
    var isNullable: Boolean = false
    var importedKey: ForeignKey? = null

    fun isSimple(): Boolean {
        return importedKey == null
    }

    fun isManyToOne(): Boolean {
        return importedKey != null
    }


    override fun toString(): String {
        return "DbColumn(name='$name', jdbcType=$jdbcType, comment=$comment, importedKey=$importedKey)"
    }
}

class ForeignKey {
    var pkTable: String = ""
    var pkColumn: String = ""
    var fkTable: String = ""
    var fkColumn: String = ""
    var onUpdate: ForeignKeyCascade = ForeignKeyCascade.none
    var onDelete: ForeignKeyCascade = ForeignKeyCascade.none
    override fun toString(): String {
        return "ForeignKey(pkTable='$pkTable', pkColumn='$pkColumn', fkTableName='$fkTable', " +
                "fkColumnName='$fkColumn', onUpdate=$onUpdate, onDelete=$onDelete)"
    }

}

enum class ForeignKeyCascade(val value: Int) {
    cascade(0),
    setDefault(1),
    setNull(2),
    none(3)
}


@Service
class GenericDbMetadataService : DbMetadataService {

    @Resource
    lateinit var  dbDialect: DbDialect

    override fun getImportedKeys(table: String): List<ForeignKey> {

        return getTableInfo(table).columns
                .values.stream()
                .filter { c -> c.importedKey != null }
                .map { c -> c.importedKey }
                .collect(Collectors.toList<ForeignKey>())
    }

    @Resource
    lateinit var jdbcTemplate: JdbcTemplate

    val tables = caseInsMapOf<DbTable>()

    override fun getTableInfo(table: String): DbTable {
        if (tables.contains(table))
            return tables[table]!!

        val dbtable = readDbMetadata(table)
        tables[table] = dbtable;

        return dbtable
    }

    private fun readDbMetadata(table: String): DbTable {

        val c = jdbcTemplate.dataSource.connection
        val md = c.metaData

        val rs = md.getTables(null, null, dbDialect.getDbIdentifier(table), null)
        rs.next()
        if (!(rs.isFirst && rs.isLast))
            throw RuntimeException("Table '${table}' not found in db");

        val pk = md.getPrimaryKeys(null, dbDialect.getCurrentScheme(), dbDialect.getDbIdentifier(table))
        var pkField: String? = null
        while (pk.next()) {
            if (pkField != null)
                throw NIY("primary key with multiple columns is not supported")
            pkField = pk.getString("COLUMN_NAME")
        }

        //читаем таблицу
        val dt = DbTable(rs.getString("TABLE_NAME"), pkField,
                rs.getString("remarks"))


        //читаем колонки
        var crs = md.getColumns(null, dbDialect.getCurrentScheme(), dbDialect.getDbIdentifier(table), null)
        while (crs.next()) {

            val column = DbColumn(crs.getString("COLUMN_NAME"),
                    JDBCType.valueOf(crs.getInt("DATA_TYPE")))

            column.size = crs.getInt("COLUMN_SIZE")
            column.ordinalPosition = crs.getInt("ORDINAL_POSITION")
            column.isNullable = crs.getBoolean("NULLABLE")
            column.comment = crs.getString("REMARKS")
            dt.addColumn(column)
        }

        //читаем внешние ключи, на которые мы ссылаемся
        crs = md.getImportedKeys(null, dbDialect.getCurrentScheme(),  dbDialect.getDbIdentifier(table))
        while (crs.next()) {
            val fk = ForeignKey()
            fk.pkTable = crs.getString("PKTABLE_NAME")
            fk.pkColumn = crs.getString("PKCOLUMN_NAME")
            fk.fkTable = crs.getString("FKTABLE_NAME")
            fk.fkColumn = crs.getString("FKCOLUMN_NAME")
            fk.onDelete = ForeignKeyCascade.values()[crs.getInt("DELETE_RULE")]
            fk.onUpdate = ForeignKeyCascade.values()[crs.getInt("UPDATE_RULE")]

            Assert.isTrue(fk.fkTable.equals(dt.name))
            dt[fk.fkColumn].importedKey = fk
        }

        //читаем связи, которые  мы экспортировали в другие таблицы -
        //и решаем - является ли связь коллекцией
        //определеяем это по каскаду
        crs = md.getExportedKeys(null, dbDialect.getCurrentScheme(),  dbDialect.getDbIdentifier(table))
        while (crs.next()) {
            val fk = ForeignKey()
            fk.pkTable = crs.getString("PKTABLE_NAME")
            fk.pkColumn = crs.getString("PKCOLUMN_NAME")
            fk.fkTable = crs.getString("FKTABLE_NAME")
            fk.fkColumn = crs.getString("FKCOLUMN_NAME")
            fk.onDelete = ForeignKeyCascade.values()[crs.getInt("DELETE_RULE")]
            fk.onUpdate = ForeignKeyCascade.values()[crs.getInt("UPDATE_RULE")]

            dt.addExprotedKey(fk)
        }

        return dt
    }

}

