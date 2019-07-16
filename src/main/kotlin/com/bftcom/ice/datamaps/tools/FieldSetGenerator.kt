package com.bftcom.ice.datamaps.tools

import com.bftcom.ice.datamaps.DataMapF
import com.bftcom.ice.datamaps.misc.Date
import com.bftcom.ice.datamaps.impl.mappings.DataField
import com.bftcom.ice.datamaps.impl.mappings.DataMappingsService
import org.springframework.stereotype.Service
import javax.annotation.Resource


@Service
class FieldSetGenerator {

    @Resource
    private
    lateinit var dataMappingsService: DataMappingsService


    fun generateFieldSet(tableName: String): String {

        val name = dataMappingsService.getEntityDefaultNameByTableName(tableName)
        val mapping = dataMappingsService.getDataMapping(name)
        var s = """
                object $name : MFS<$name>() {
                        val entity = "$name"
                        val table = "$tableName"
            """
        s += "\r\n"
        mapping.fields.values.forEach { it ->
            s += "                        ${buildField(it)}\r\n"
        }
        s += """
                }
            """
        return s
    }

    private fun buildField(field: DataField): String {
        return when {
            field.kotlinType == Boolean::class -> "val ${field.name} = Field.boolean(\"${field.name}\")"
            field.kotlinType == String::class -> "val ${field.name} = Field.string(\"${field.name}\")"
            field.kotlinType == Long::class -> "val ${field.name} = Field.long(\"${field.name}\")"
            field.kotlinType == Int::class -> "val ${field.name} = Field.int(\"${field.name}\")"
            field.kotlinType == Date::class -> "val ${field.name} = Field.date(\"${field.name}\")"
            field.kotlinType == DataMapF::class -> "val ${field.name} = Field.reference(\"${field.name}\")"
            field.is1N() -> "val ${field.name} = Field.list(\"${field.name}\", ${field.oneToMany().to})"
            else -> ""
        }
    }
}