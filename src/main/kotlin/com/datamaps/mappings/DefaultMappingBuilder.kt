package com.datamaps.mappings

import com.datamaps.services.DbColumn
import com.datamaps.services.DbMetadataService
import com.datamaps.services.DbTable
import org.springframework.stereotype.Service
import java.sql.JDBCType
import javax.annotation.Resource

/**
 * Created by b.schukin on 07.11.2017.
 */


@Service
class DefaultMappingBuilder {
    @Resource
    lateinit var dbMetadataService: DbMetadataService

    fun buildDefault(table:String ): DataMapping {

        var dbtable = dbMetadataService.getTableInfo(table)
        return buildDefault(dbtable)

    }

    fun buildDefault(table: DbTable): DataMapping {
        val dm = DataMapping(table.name, table.name)
        dm.idColumn = table.primaryKeyField
        dm.scanFieldsInDb = true

        //обратываем простые колонки, с ними то просто
        table.simpleColumns.forEach { col ->
            val df = DataField(getDefaultFieldName(col))
            df.sqlcolumn = col.name
            df.javaType = getJavaTypeByJDBCType(col.jdbcType)

            dm.add(df)

            //включаем в FULL-группу - здесь лежать все поля
            dm.fullGroup.add(df.name)

            //включаем в дефолтную группу - если поле не участвует в ссылке и не является каким нето клобом
            if(defaultGroupCandidate(col))
                dm.defaultGroup.add(df.name)
        }

        //обратываем M-1 колонки, с ними то посложнее
        table.m1Columns.forEach { col ->
            val df = DataField(getDefaultFieldName(col))
            df.sqlcolumn = col.name
            df.javaType = getJavaTypeByJDBCType(col.jdbcType)
            df.manyToOne = ManyToOne(col.importedKey!!.pkTable, col.importedKey!!.pkColumn)

            //включаем в FULL-группу - здесь лежать все поля
            dm.fullGroup.add(df.name)
            dm.add(df)
        }

        return dm
    }
}


/*можно ли влюкчить поле в дефолтную группу*/
fun defaultGroupCandidate(col: DbColumn): Boolean {
    if (!col.isSimple())
        return false;

    return when (col.jdbcType) {
        JDBCType.BINARY, JDBCType.VARBINARY, JDBCType.LONGVARBINARY,
        JDBCType.CLOB, JDBCType.BLOB,
        JDBCType.ARRAY, JDBCType.DISTINCT, JDBCType.DATALINK, JDBCType.STRUCT, JDBCType.REF -> false
        else -> true
    }
}

fun getDefaultFieldName(col: DbColumn): String {
    return when (col.importedKey) {
        null -> col.name

        else -> { // Note the block
            if (col.name.endsWith("id", true))
                col.name.substring(0, col.name.length - 2) else col.name
        }
    }
}