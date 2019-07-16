package com.bftcom.ice.server.tools.dbsync

import com.bftcom.ice.common.maps.Field
import com.bftcom.ice.common.maps.FieldSetRepo
import com.bftcom.ice.server.datamaps.mappings.DataField
import com.bftcom.ice.server.datamaps.mappings.DataMapping
import com.bftcom.ice.server.datamaps.mappings.DataMappingsService
import com.bftcom.ice.server.util.getJDBCTypeByFieldType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import kotlin.reflect.KClass

//здесь проводятся сравнения между БД и сервером и репортятся различия

@Service
class DbDiffService {
    /*@Autowired
    private lateinit var fieldSetRepository: FieldSetRepo*/

    @Autowired
    private lateinit var dataMappingsService: DataMappingsService

    fun makeDiffForOneEntity(entity: String, clearDbMetada: Boolean = false): EntityDbDiff {
        if(clearDbMetada)
            dataMappingsService.removeDataMapping(entity)

        //получаем маппинг, построенный по БД
        val mapping = dataMappingsService.getDefaultDataMapping(entity, false)

        return makeDiffForOneEntity(entity, mapping)
    }

    fun makeDiffForOneEntity(entity: String, mapping: DataMapping?): EntityDbDiff {

        //и маппинг описанный  в фиелдсете
        val fieldSet = FieldSetRepo.fieldSet(entity)
        val fields = FieldSetRepo.getStaticFields(fieldSet)

        return makeEntityDbDiff(entity, fields, mapping)
    }

    fun makeEntityDbDiff(entity:String, fields: Map<String, Field<*, *>>, mapping: DataMapping?): EntityDbDiff {

        if(mapping==null)//это характнрно для новых таблиц
            return EntityDbDiff(entity, fields.values.toList(), isNew = true)
        //строим разницу.
        //сначала переименованные поля
        //это поля name котрых отсуствует в полях дб-маппинга
        //но prevName присутствует
        //карта: новое имя, старое имя
        val renamedFields = fields
                .filter {
                    it.value.prevFieldName != null && mapping.fields.containsKey(it.value.prevFieldName!!) &&
                            fields.containsKey(it.key)
                }.map { it.key to it.value.prevFieldName!!.toUpperCase() }.toMap()

        //поля которые есть только в бД: те поля которых нет в в фиелдсете и нет в переименнованных
        val fieldsOnlyInDb = mapping.fields
                .filterKeys { !renamedFields.values.contains(it.toUpperCase()) }//отсекаем те что попади в переименования
                .filterKeys { !fields.map { it.key.toLowerCase() }.toSet().contains(it.toLowerCase()) }
                .filterKeys { !mapping.fields[it]!!.isBackReference }
                .values.toList()

        //поля которые есть только в мапинге: их нет в БД и нет в переименнованных
        val fieldsOnlyInMap = fields
                .filterKeys { !renamedFields.contains(it) }//отсекаем те что попади в переименования
                .filterKeys { key -> !mapping.fields.map { it.key.toLowerCase() }.toSet().contains(key.toLowerCase()) }
                .values.toList()

        //измененные поля: те у коотрых поменяли имя или общие поля, у которых чтото поменяли внутре
        val changedFields =
                fields
                        .filter { !it.value.synthetic }
                        .filterKeys { key -> mapping.fields.map{it.key.toLowerCase()}.toSet().contains(key.toLowerCase()) }
                .map { findChange(mapping.fields[it.key]!!, it.value) }
                .union(
                        renamedFields.map { findChange(mapping.fields[it.value]!!, fields[it.key]!!) }
                )
                .union(
                        fields.filter { !it.value.synthetic }
                                .filterKeys { key -> fields[key]!!.thisJoinColumn!=null }
                                .map { findChange(mapping.findByDbColumnName(fields[it.key]!!.thisJoinColumn!!)!!,
                                        fields[it.key]!!) }
                )
                .filterNotNull()


        return EntityDbDiff(entity, fieldsOnlyInDb, fieldsOnlyInMap, changedFields)
    }


    private fun findChange(columnField: DataField, fsField: Field<*, *>): EntityDbDiff.ColumnChange? {
        val nameChange =
                if (columnField.name.equals(fsField.fieldName, true)) null
                else if(fsField.thisJoinColumn!=null && columnField.sqlcolumn.equals(fsField.thisJoinColumn, true) ) null
                else EntityDbDiff.NameChange(fsField.prevFieldName!!, fsField.fieldName)

        val typeChange =
                if ((columnField.jdbcType==fsField.fieldType!!.getJDBCTypeByFieldType() || !columnField.isSimple())
                && !fsField.isEnum()) null
                else EntityDbDiff.TypeChange(fsField.fieldName, fsField.thisJoinColumn, columnField.kotlinType, fsField.type() as KClass<*>)

        val flag = nameChange != null  || typeChange!=null
        return if (flag) EntityDbDiff.ColumnChange(nameChange, typeChange) else null
    }
}


data class EntityDbDiff(val entity: String, val fieldsOnlyInDb: List<DataField>,
                        val fieldsOnlyInDataMap: List<Field<*, *>>, val changedFields: List<ColumnChange>,
                        val isNew: Boolean = false) {

    constructor(aentity: String, afieldsOnlyInDataMap: List<Field<*, *>>, isNew: Boolean = true)
            :this(aentity, emptyList(), afieldsOnlyInDataMap, emptyList(), true)

    class ColumnChange(val nameChange: NameChange?, val typeChange: TypeChange?)

    class TypeChange(val name:String, val joinColumn:String?, val inDb: KClass<*>, val inMapping: KClass<*>)

    class NameChange(val inDb: String, val inMapping: String)

    fun isEmpty() = fieldsOnlyInDb.isEmpty() && fieldsOnlyInDataMap.isEmpty() && changedFields.isEmpty()

    override fun toString(): String {
        if(isEmpty())
        {
            return  "$entity: entity-db diff{ \r\n\t NO DIFF \t\r\n}"
        }
        var str  =  "$entity: entity - db diff{ \r\n"
        str += "\tfields only in Db: = {"
        str += if(fieldsOnlyInDb.isEmpty())  "" else
            ("\r\n\t\t"  + fieldsOnlyInDb.joinToString("\r\n\t\t") + "\r\n\t")
        str += "}"

        str += "\r\n\tfields only in entity: = {"
        str += if(fieldsOnlyInDataMap.isEmpty())  "" else
            ("\r\n\t\t"  + fieldsOnlyInDataMap.joinToString("\r\n\t\t") + "\r\n\t")
        str += "}"

        str += "\r\n\tchanged fields : = {"
        str += if(changedFields.isEmpty())  "" else
            ("\r\n\t\t"  + changedFields.joinToString("\r\n\t\t") + "\r\n\t")
        str += "}"


        return "$str\r\n}"
    }
}