package com.bftcom.ice.datamaps.core.dbsync

import com.bftcom.ice.datamaps.DataMap
import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.SomeFieldSet
import com.bftcom.ice.datamaps.UndefinedFieldSet
import com.bftcom.ice.datamaps.core.mappings.DataMappingsService
import com.bftcom.ice.datamaps.core.mappings.NameMappingsStrategy
import com.bftcom.ice.datamaps.core.dialects.DbDialect
import com.bftcom.ice.datamaps.core.mappings.ForeignKeyCascade
import com.bftcom.ice.datamaps.core.util.getJDBCTypeByFieldType
import com.bftcom.ice.datamaps.misc.FieldSetRepo
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.sql.JDBCType
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass


@Service
open class DbScriptMaker {
    @Autowired
    protected lateinit var dataMappingsService: DataMappingsService

    @Autowired
    protected lateinit var dbDialect: DbDialect


    @Autowired
    protected lateinit var nameMappingsStrategy: NameMappingsStrategy


    companion object {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
    }


    fun makeScriptsAndApendToFile(diff: EntityDbDiff, file: File, appendTofile: Boolean = true,
                                  deleteColumnsFromFb: Boolean = true): String {
        val sql = makeScript(diff, deleteColumnsFromFb, true, true)
        if (!sql.isBlank() && appendTofile) {
            val header = System.lineSeparator() + "-- appended at [${dateFormat.format(Date())}]" + System.lineSeparator()
            file.appendText(header + sql)
        }
        return sql
    }


    fun makeScript(diff: EntityDbDiff,
                   deleteColumns: Boolean = true,
                   createColumns: Boolean = true,
                   alterColumns: Boolean = true): String {

        val table = dataMappingsService.getTableNameByEntity(diff.entity)

        val script = StringBuilder()

        if (diff.isNew)
            makeCreateTableScripts(table, script, diff)

        if (createColumns)
            makeCreateColumnsScript(table, script, diff)

        if (deleteColumns)
            makeDeleteColumns(table, script, diff)

        return script.toString()
    }

    protected fun makeCreateTableScripts(table: String, script: StringBuilder, diff: EntityDbDiff) {
        val idfield = diff.fieldsOnlyInDataMap.find { it.fieldName == DataMap.ID }!!

        script.append(dbDialect.ddlCreateTableWithIdentity(table, idfield.n, idfield.type() as KClass<*>))
    }

    private fun makeDeleteColumns(table: String, script: StringBuilder, diff: EntityDbDiff) {
        diff.fieldsOnlyInDb.filter { it.sqlcolumn != null }
                .forEach {
                    script.append(dbDialect.ddlDropColumn(table, it.sqlcolumn!!))
                }
    }


    fun makeCreateColumnsScript(table: String, script: StringBuilder, diff: EntityDbDiff) {
        filterColumnsToCreate(diff.fieldsOnlyInDataMap).forEach {

            val newscript = when {
                it.isReference() -> {
                    buildReferenceColumnDdl(it, table)
                }
                //для списка, колонку мы добавляем не в @table а в таблицу, объекты которой содержит наш список
                it.isList() -> {
                    buildListColumnDdl(it, table)
                }
                else -> {
                    val newColumnName = nameMappingsStrategy.getDbColumnName(it.fieldName)
                    dbDialect.ddlAddScalarColumn(table, newColumnName,
                            it.fieldType!!.getJDBCTypeByFieldType(), it.length, it.required)
                }
            }
            script.append(newscript)
        }
    }

    protected open fun filterColumnsToCreate(list: List<Field<*, *>>): List<Field<*, *>> {
        //айдишники не вставляем: они просставяются в самой таблице
        return list.filter { !DataMap.ID.equals(it.fieldName, true) }
    }

    protected open fun buildListColumnDdl(f: Field<*, *>, table: String): String {
        val refFieldSet = FieldSetRepo.fieldSet((f.refFieldSet()).entity)
        val refMapping = dataMappingsService.getDataMapping(refFieldSet.entity)
        val mapping = dataMappingsService.getDataMapping(dataMappingsService.getEntityDefaultNameByTableName(table))

        var newColumnName = nameMappingsStrategy.getDbColumnName(mapping.table + "Id")
        if (refMapping.findByDbColumnName(newColumnName) != null)
            newColumnName = nameMappingsStrategy.getDefaultCollectionColumnName(f.fieldName)

        return dbDialect.ddlAddReferenceColumn(refMapping.table, newColumnName, JDBCType.BIGINT,
                table, refMapping.idColumn!!, ForeignKeyCascade.Cascade)
    }


    protected open fun buildReferenceColumnDdl(f: Field<*, *>, table: String): String {
        val refFieldSet =
                if (f.referencedEntity != null && (f.refFieldSet() == SomeFieldSet || f.refFieldSet() == UndefinedFieldSet))
                    FieldSetRepo.fieldSet(f.referencedEntity!!)
                else FieldSetRepo.fieldSet((f.refFieldSet()).entity)
        val refMapping = dataMappingsService.getDataMapping(refFieldSet.entity)
        val newColumnName = nameMappingsStrategy.getDbColumnName(f.n + StringUtils.capitalize(refMapping.idField()!!.name))
        val jdbcType = refMapping.idField()!!.jdbcType
        return dbDialect.ddlAddReferenceColumn(table, newColumnName, jdbcType, refMapping.table, refMapping.idColumn!!)
    }


}