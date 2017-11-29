package com.datamaps.mappings

import com.datamaps.services.DbColumn
import com.datamaps.services.DbMetadataService
import com.datamaps.services.DbTable
import com.datamaps.services.ForeignKey
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

    @Resource
    lateinit var nameMappingsStrategy: NameMappingsStrategy

    fun buildDefault(table: String): DataMapping {

        val dbtable = dbMetadataService.getTableInfo(table)
        return buildDefault(dbtable)

    }

    fun buildDefault(table: DbTable): DataMapping {
        val dm = DataMapping(getDefaultEntityName(table.name), table.name)
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
            if (defaultGroupCandidate(col))
                dm.defaultGroup.add(df.name)
        }

        //обратываем M-1 колонки, с ними то посложнее
        table.m1Columns.forEach { col ->
            val df = DataField(getDefaultFieldName(col))
            df.sqlcolumn = col.name
            df.javaType = getJavaTypeByJDBCType(col.jdbcType)
            df.manyToOne = ManyToOne(
                    nameMappingsStrategy.getJavaEntityName(col.importedKey!!.pkTable),
                    col.importedKey!!.pkColumn)

            //включаем в группу ссылок
            dm.refsGroup.add(df.name)
            //включаем в FULL-группу - здесь лежать все поля
            dm.fullGroup.add(df.name)
            dm.add(df)
        }

        //обратываем М- колонки, с ними то еще посложнее
        table.oneToManyCollections.forEach { fk ->
            val df = DataField(getDefaultCollectionName(fk))
            df.javaType = List::class.java
            df.oneToMany = OneToMany(
                    nameMappingsStrategy.getJavaEntityName(fk.fkTable),
                    fk.fkColumn)

            //включаем в FULL-группу - здесь лежат все поля
            dm.fullGroup.add(df.name)
            dm.lists.add(df.name)
            dm.add(df)
        }

        return dm
    }


    fun getDefaultEntityName(name: String): String {
        return nameMappingsStrategy.getJavaEntityName(name)
    }

    fun getDefaultFieldName(col: DbColumn): String {
        return nameMappingsStrategy.getJavaPropertyName(escapeId(col.name))
    }

    fun getDefaultCollectionName(fk: ForeignKey): String {

        val column = nameMappingsStrategy.getJavaPropertyName(escapeId(fk.fkTable))

        return when (column.last()) {
            's' -> column + "es"
            else -> column + "s"
        }
    }
}


/*можно ли влюкчить поле в дефолтную группу*/
fun defaultGroupCandidate(col: DbColumn): Boolean {
    if (!col.isSimple())
        return false

    return when (col.jdbcType) {
        JDBCType.BINARY, JDBCType.VARBINARY, JDBCType.LONGVARBINARY,
        JDBCType.CLOB, JDBCType.BLOB,
        JDBCType.ARRAY, JDBCType.DISTINCT, JDBCType.DATALINK, JDBCType.STRUCT, JDBCType.REF -> false
        else -> true
    }
}


private fun escapeId(name: String): String {
    return when {
        name.equals("id", true) -> name
        name.endsWith("_id", true) -> name.substring(0, name.length - 3)
        name.endsWith("id", true) -> name.substring(0, name.length - 2)
        else -> name
    }

}

