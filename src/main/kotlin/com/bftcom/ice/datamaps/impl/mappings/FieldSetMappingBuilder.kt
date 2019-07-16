package com.bftcom.ice.datamaps.impl.mappings

import com.bftcom.ice.datamaps.DataMapF
import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.FieldSet
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.datamaps.utils.FieldType
import com.bftcom.ice.datamaps.common.maps.*
import com.bftcom.ice.datamaps.tools.dbsync.DbDiffService
import com.bftcom.ice.datamaps.tools.dbsync.EntityDbDiff
import com.bftcom.ice.datamaps.impl.util.getJDBCTypeByFieldType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.sql.JDBCType
import java.util.*
import javax.annotation.Resource
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf

/**
 * Created by b.schukin on 07.11.2017.
 *
 * Отвечает за построение маппинга по на основе фиелдсета.
 * На входе: "маппинг по умолчанию", построенный по базе и фиелдсет ("кастомный" маппинг).
 * На выходе: результат мержа  маппингов
 *
 *
 */


@Service
class FieldSetMappingBuilder {

    @Autowired
    lateinit var dbDiffService: DbDiffService

    @Resource
    private lateinit var nameMappingsStrategy: NameMappingsStrategy

    @Resource
    private lateinit var dbMetadataService: DbMetadataService

    fun buildMappingBasedOnFieldSet(entityName: String, dataMapping: DataMapping): DataMapping {

        //и маппинг описанный  в фиелдсете
        val fieldSet = FieldSetRepo.fieldSetOrNull(entityName) ?: return dataMapping.copy()

        //val entityName = fieldSet.entity
        val result = dataMapping.copy(name = entityName)

        val diff = dbDiffService.makeDiffForOneEntity(entityName, dataMapping)

        applyChangedFields(result, fieldSet, diff.changedFields)
        applyDataMapAddedFields(result, diff.fieldsOnlyInDataMap)

        applyOnlyImplicitlyDeclaredFields(result, diff.fieldsOnlyInDb)

        return result
    }

    private fun applyChangedFields(result: DataMapping, fieldSet: FieldSet,
                                   changedFields: List<EntityDbDiff.ColumnChange>) {

        changedFields.filter {
            it.typeChange != null
        }.forEach {
            applyIdToReferenceChangeIfShould(fieldSet, it.typeChange!!, result)
            applyStringToJsonDatamapChangeIfShould(fieldSet, it.typeChange, result)
            applyStringToEnumDatamapChangeIfShould(fieldSet, it.typeChange, result)
            //перемапиливание числа на буль
            applyNumberToBoolChangeIfShould(it.typeChange, result)
        }
    }



    private fun applyDataMapAddedFields(result: DataMapping, addedFields: List<Field<*, *>>) {

        addedFields.forEach {
            applyListAddIfShould(it, result)
            applyAddedReferenceIfShould(it, result)
            applySqlFormulaIfShould(it, result)
        }
    }

    /***
     * В группах refs, collection, full должны находиться только явно специфицрованные в fieldset поля
     */
    private fun applyOnlyImplicitlyDeclaredFields(result: DataMapping, fieldsOnlyInDb: List<DataField>) {

        fieldsOnlyInDb.forEach {
            val mappedField = result.findByName(it.name)
            mappedField?.let {
                when {
                    mappedField.isBackReference || result.idColumn==mappedField.sqlcolumn -> {
                        return@let
                    }
                    mappedField.is1N() -> {
                        //меняем группы
                        result.fullGroup.remove(mappedField.name)
                        result.listsGroup.remove(mappedField.name)
                    }
                    mappedField.isM1() -> {
                        //меняем группы
                        result.fullGroup.remove(mappedField.name)
                        result.refsGroup.remove(mappedField.name)
                    }
                    mappedField.isSimple() -> {
                        //меняем группы
                        result.fullGroup.remove(mappedField.name)
                        result.defaultGroup.remove(mappedField.name)
                    }
                    mappedField.isJson() -> {
                        //меняем группы
                        result.fullGroup.remove(mappedField.name)
                        result.blobsGroup.remove(mappedField.name)
                    }

                }
            }
        }

    }

    private fun applyListAddIfShould(field: Field<*, *>,
                                     result: DataMapping) {
        if (!addedFieldIsList(field))
            return

        //инфа по ссылаемой колонке
        //получаем точное наименование в базе ссылаемой колонки
        val referenced = field.refFieldSet()
        val table2 = nameMappingsStrategy.getDefaultDbTableName(referenced.entity)
        val tableInfo2 = dbMetadataService.getTableInfo(table2)
        val col2 = tableInfo2[field.thatJoinColumn!!].name

        //инфа по родительской ссылке
        //получаем точное наименование в базе ссылаемой колонки
        val tableInfo1 = dbMetadataService.getTableInfo(result.table)
        val col1 = if(field.thisJoinColumn==null)
            tableInfo1.primaryKeyField else tableInfo1[field.thisJoinColumn!!].name


        val oneToMany = OneToMany(referenced.entity, col1, col2)
        val newField = DataField(field.n, null,
                "", oneToMany, List::class, JDBCType.OTHER)

        //удаляем замапленную с именем по умолчанию коллекцию из маппинга
        val oldField = result.findByReference(oneToMany)
        oldField?.let {
            result.remove(oldField.name)
        }

        //добавляем новую коллекцию
        result.fields[newField.name] = newField
        result.listsGroup.add(newField.name)

    }

    private fun applyAddedReferenceIfShould(field: Field<*, *>,
                                            result: DataMapping) {
        if (!addedFieldIsRef(field))
            return

        //получаем точное наименование в базе ссылаемой колонки
        val referenced = field.refFieldSet()
        val table2 = nameMappingsStrategy.getDefaultDbTableName(referenced.entity)
        val tableInfo2 = dbMetadataService.getTableInfo(table2)
        val col2 = if(field.thatJoinColumn==null) tableInfo2.primaryKeyField else
                        tableInfo2[field.thatJoinColumn!!].name
        val colType = tableInfo2.columns[col2]!!.jdbcType

        //получаем точное наименование в базе ссылаемой колонки
        val tableInfo1 = dbMetadataService.getTableInfo(result.table)
        val col1 = tableInfo1[field.thisJoinColumn!!].name

        val manyToOne = ManyToOne(referenced.entity, col1, col2)

        val newField = DataField(field.n, field.thisJoinColumn!!,
                "", manyToOne, DataMapF::class, colType)

        //добавляем новую ссылку
        result.fields[newField.name] = newField
        result.refsGroup.add(newField.name)

    }

    private fun applySqlFormulaIfShould(field: Field<*, *>,
                                        result: DataMapping) {
        if (!addedFieldIsFormula(field))
            return

        val newField =
                if (field.isReference() || field.isList())
                    DataField(field.fieldName, sqlcolumn = null,
                            description = field.caption,
                            reference = null, kotlinType = field.type() as KClass<*>,
                            jdbcType = field.fieldType!!.getJDBCTypeByFieldType(),
                            oqlFormula = OqlFormula(field.oqlFormula!!,
                                    lateralTable = field.referencedEntity,
                                    isList = field.isList()))
                else
                    DataField(field.fieldName, sqlcolumn = null,
                            description = field.caption,
                            reference = null, kotlinType = field.type() as KClass<*>,
                            jdbcType = field.fieldType!!.getJDBCTypeByFieldType(),
                            oqlFormula = OqlFormula(field.oqlFormula!!))

        result.fields[newField.name] = newField
        result.formulasGroup.add(newField.name)
        result.fullGroup.add(newField.name)

    }

    private fun applyIdToReferenceChangeIfShould(fieldSet: FieldSet,
                                                 typeChange: EntityDbDiff.TypeChange,
                                                 result: DataMapping) {

        val f = FieldSetRepo.getFieldOrNull(fieldSet, typeChange.name)!!

        if (typeChangedFromScalarToDataMap(typeChange)) {

            val referenced = f.refFieldSet()
            val table = nameMappingsStrategy.getDefaultDbTableName(referenced.entity)
            val tableInfo = dbMetadataService.getTableInfo(table)

            val oldField = if (typeChange.joinColumn != null)
                result.findByDbColumnName(typeChange.joinColumn)!!
            else result[typeChange.name]

            val manyToOne = ManyToOne(referenced.entity, oldField.sqlcolumn!!, tableInfo.primaryKeyField)//ещвщ primaryKeyField -
            val newField = DataField(typeChange.name, oldField.sqlcolumn,
                    oldField.description, manyToOne, DataMapF::class, tableInfo[tableInfo.primaryKeyField].jdbcType)

            applyChangeInMapping(result, oldField, newField, result.refsGroup)
        }
    }


    private fun applyStringToJsonDatamapChangeIfShould(fieldSet: FieldSet,
                                                       typeChange: EntityDbDiff.TypeChange, result: DataMapping) {
        val f = FieldSetRepo.getFieldOrNull(fieldSet, typeChange.name) ?: return
        if (typeChangedFromStringToDataMapJson(typeChange, f)) {

            val oldField = result[typeChange.name]

            val newField = DataField(oldField.name, oldField.sqlcolumn,
                    oldField.description, oldField.reference,
                    DataMapF::class, JDBCType.STRUCT)

            applyChangeInMapping(result, oldField, newField, result.blobsGroup)
        }
    }

    private fun applyStringToEnumDatamapChangeIfShould(fieldSet: FieldSet,
                                                       typeChange: EntityDbDiff.TypeChange, result: DataMapping)
    {
        val f = FieldSetRepo.getFieldOrNull(fieldSet, typeChange.name) ?: return
        if((f.type() as KClass<*>).isSubclassOf(Enum::class))
        {
            val oldField = result[typeChange.name]

            val newField = DataField(oldField.name, oldField.sqlcolumn,
                    oldField.description, oldField.reference,
                    f.type() as KClass<*>, oldField.jdbcType)

            applyChangeInMapping(result, oldField, newField, result.defaultGroup)
        }
    }

    private fun applyNumberToBoolChangeIfShould(typeChange: EntityDbDiff.TypeChange, result: DataMapping) {
        if(typeChange.inMapping==Boolean::class && typeChange.inDb.isSubclassOf(Number::class))
        {
            val oldField = result[typeChange.name]
            val newField = DataField(oldField.name, oldField.sqlcolumn,
                    oldField.description, oldField.reference,
                    typeChange.inMapping, oldField.jdbcType)
            applyChangeInMapping(result, oldField, newField, result.defaultGroup)
        }
    }


    private fun typeChangedFromStringToDataMapJson(change: EntityDbDiff.TypeChange, f: Field<*, *>): Boolean {

        return String::class.isSuperclassOf(change.inDb) &&
                (DataMapF::class.isSuperclassOf(change.inMapping) || MFS::class.isSuperclassOf(change.inMapping))
                &&
                f.fieldType == FieldType.Json
    }

    private fun typeChangedFromScalarToDataMap(change: EntityDbDiff.TypeChange): Boolean {

        return (Number::class.isSuperclassOf(change.inDb) ||
                UUID::class == change.inDb /*|| String::class == change.inDb*/) &&
                FieldSet::class.isSuperclassOf(change.inMapping)
    }

    private fun addedFieldIsList(f: Field<*, *>): Boolean {

        return f.isList() && f.thatJoinColumn!= null
    }

    private fun addedFieldIsRef(f: Field<*, *>): Boolean {

        return f.isReference() &&
                (f.thatJoinColumn!= null || f.thisJoinColumn!=null)
    }

    private fun addedFieldIsFormula(f: Field<*, *>) = f.oqlFormula != null

    private fun applyChangeInMapping(result: DataMapping, oldField: DataField, newField: DataField, vararg groupsToAdd: DataGroup) {
        //меняем группы
        result.fields.remove(oldField.name)
        result.defaultGroup.remove(oldField.name)
        result.fullGroup.remove(oldField.name)

        result.fields[newField.name] = newField

        groupsToAdd.forEach {
            it.add(newField.name)
        }

        result.fullGroup.add(newField.name)
    }
}


