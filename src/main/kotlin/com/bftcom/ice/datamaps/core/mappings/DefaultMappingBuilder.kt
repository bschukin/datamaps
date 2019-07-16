package com.bftcom.ice.datamaps.core.mappings

import com.bftcom.ice.datamaps.misc.SomethingNotFound
import com.bftcom.ice.datamaps.misc.makeSure
import com.bftcom.ice.datamaps.DataMapF
import com.bftcom.ice.datamaps.core.util.SequenceIncrementor
import com.bftcom.ice.datamaps.core.util.getJavaTypeByJDBCType
import org.springframework.stereotype.Service
import java.sql.JDBCType
import javax.annotation.Resource

/**
 * Created by b.schukin on 07.11.2017.
 */


@Service
class DefaultMappingBuilder {
    @Resource
    private lateinit var dbMetadataService: DbMetadataService

    @Resource
    private lateinit var nameMappingsStrategy: NameMappingsStrategy

    @Resource
    private lateinit var sequenceIncrementor: SequenceIncrementor

    fun buildDefault(table: String): DataMapping {

        return   buildDefault(table, false)
                ?: throw SomethingNotFound("table [$table] is not found in db")
    }

    fun buildDefault(table: String, throwIfNotFound: Boolean = true): DataMapping? {

        val dbtable = dbMetadataService.getTableInfo(table, throwIfNotFound)

        if(dbtable==null)
            return null//если нужно throwIfNotFound - то выкинется из getTableInfo

        return buildDefault(dbtable)
    }

    private fun buildDefault(table: DbTable): DataMapping {

        //тип генерации ID
        val idGenerationType =
                when {
                    table[table.primaryKeyField].isAutoIncrement -> IdGenerationType.IDENTITY
                    sequenceIncrementor.canGenerateIdFromSequence(table.name) -> IdGenerationType.SEQUENCE
                    else -> IdGenerationType.NONE
                }

        val dm = DataMapping(getDefaultEntityName(table.name), table.name,
                table.primaryKeyField, idGenerationType)

        //обратываем простые колонки, с ними то просто
        table.simpleColumns.forEach { col ->

            val type = getJavaTypeByJDBCType(col.jdbcType, col.size, col.decimalDigits)
            val df = DataField(nameMappingsStrategy.getJavaPropertyEscapedIdName(col.name, escapeId = false),
                    col.name, col.comment.orEmpty(), null, type, col.jdbcType)

            dm.add(df)

            //включаем в FULL-группу - здесь лежат все поля (NB) кроме блобов
            if(!col.isBlob())
                dm.fullGroup.add(df.name)

            //включаем в дефолтную группу - если поле не участвует в ссылке и не является каким нето клобом
            if (defaultGroupCandidate(col))
                dm.defaultGroup.add(df.name)

            //если клоб - включаем в группу клобов
            if (col.isBlob() || col.isJson())
                dm.blobsGroup.add(df.name)
        }

        //обратываем M-1 колонки, с ними то посложнее
        table.m1Columns.forEach { col ->
            val type =  DataMapF::class
            makeSure(col.name == col.importedKey!!.fkColumn)
            val manyToOne = ManyToOne(
                    nameMappingsStrategy.getJavaEntityName(col.importedKey!!.pkTable),
                    col.importedKey!!.fkColumn,
                    col.importedKey!!.pkColumn)

            val df = DataField(nameMappingsStrategy.getJavaPropertyEscapedIdName(col.name),
                    col.name, col.comment.orEmpty(), manyToOne, type, col.jdbcType, col.isBackReference())

            //включаем в группу ссылок
            dm.refsGroup.add(df.name)
            //включаем в FULL-группу - здесь лежать все поля
            dm.fullGroup.add(df.name)
            dm.add(df)
        }

        //обратываем 1-N колонки, с ними то еще посложнее
        table.oneToManyCollections.forEach { fk ->

            var name = nameMappingsStrategy.getDefaultCollectionName(fk.fkTable)
            var counter = 1
            while (dm.fields.containsKey(name)) {
                name =  nameMappingsStrategy.getDefaultCollectionName(fk.fkTable, counter)
                counter++
            }

            val type = List::class
            val oneToMany = OneToMany(
                    nameMappingsStrategy.getJavaEntityName(fk.fkTable),
                    fk.pkColumn, fk.fkColumn)
            val df = DataField(name, null, "", oneToMany, type, JDBCType.OTHER)


            //включаем в FULL-группу - здесь лежат все поля
            dm.fullGroup.add(df.name)
            dm.listsGroup.add(df.name)
            dm.add(df)
        }

        return dm
    }


    private fun getDefaultEntityName(name: String): String {
        return nameMappingsStrategy.getJavaEntityName(name)
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




