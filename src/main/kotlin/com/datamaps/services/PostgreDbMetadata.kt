package com.datamaps.services

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import java.sql.JDBCType
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 *
 * Метаданные таблиц БД (обертка над JDBC-метаданными базы)
 */


class DbTable(val name: String, val comment: String?) {
    var columns = linkedMapOf<String, DbColumn>()
    override fun toString(): String {
        return "DbTable(name='$name', comment=$comment,\n\t\t columns=$columns)"
    }

    public operator fun get(column: String): DbColumn {
        if(!columns.containsKey(column.toLowerCase()))
            throw RuntimeException()
        return columns.get(column.toLowerCase())!!
    }
    public fun addColumn(value:DbColumn ) { columns[value.name.toLowerCase()] = value}
}

class DbColumn(val name: String, val jdbcType: JDBCType ) {
    var ordinalPosition: Int = -1
    var size: Int = 0
    var comment: String? = ""
    var isNullable: Boolean = false
    var importedKey: ForeignKey? = null

    override fun toString(): String {
        return "DbColumn(name='$name', jdbcType=$jdbcType, comment=$comment, importedKey=$importedKey)"
    }
}

class ForeignKey
{
    var pkTable:String = ""
    var pkColumn:String = ""
    var fkTable:String = ""
    var fkColumn:String = ""
    var onUpdate:ForeignKeyCascade = ForeignKeyCascade.none
    var onDelete:ForeignKeyCascade = ForeignKeyCascade.none
    override fun toString(): String {
        return "ForeignKey(pkTable='$pkTable', pkColumn='$pkColumn', fkTableName='$fkTable', " +
                "fkColumnName='$fkColumn', onUpdate=$onUpdate, onDelete=$onDelete)"
    }

}

enum class ForeignKeyCascade(val value:Int)
{
    cascade(0),
    setDefault(1),
    setNull(2),
    none(3)
}

interface DbMetadataService {

}

@Service
class GenericDbMetadataService : DbMetadataService {
    @Resource
    lateinit var jdbcTemplate: JdbcTemplate

    val tables = hashMapOf<String, DbTable>()

    public fun getTableInfo(table: String): DbTable {
        var key = table.toLowerCase()
        if (tables.contains(key))
            return tables[key]!!

        val dbtable = readDbMetadata(key)
        tables[key] = dbtable;

        return dbtable;
    }

    private fun readDbMetadata(table: String): DbTable {

        val c = jdbcTemplate.dataSource.connection
        val md = c.metaData

        var rs = md.getTables(null, "PUBLIC", table.toUpperCase(), null)
        rs.next()
        Assert.isTrue(rs.isFirst && rs.isLast)

        //читаем таблицу
        val dt = DbTable(rs.getString("TABLE_NAME"),
                rs.getString("remarks"))

        //читаем колонки
        var crs = md.getColumns(null, "PUBLIC", table.toUpperCase(), null)
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
        crs = md.getImportedKeys(null, "PUBLIC", table.toUpperCase())
        while (crs.next()) {
            val fk = ForeignKey()
            fk.pkTable = crs.getString("PKTABLE_NAME")
            fk.pkColumn = crs.getString("PKCOLUMN_NAME")
            fk.fkTable = crs.getString("FKTABLE_NAME")
            fk.fkColumn = crs.getString("FKCOLUMN_NAME")
            //print(crs.getString("FK_NAME").toString() + " ")
            //print(crs.getString("PK_NAME") + " ")
            fk.onDelete = ForeignKeyCascade.values()[crs.getInt("DELETE_RULE")]
            fk.onUpdate = ForeignKeyCascade.values()[crs.getInt("UPDATE_RULE")]

            Assert.isTrue(fk.fkTable.equals(dt.name))
            dt[fk.fkColumn].importedKey = fk
        }
        return dt
    }

}

