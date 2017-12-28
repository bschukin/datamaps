package com.datamaps.tools

import com.datamaps.mappings.DataField
import com.datamaps.mappings.DataMappingsService
import com.datamaps.maps.DataMap
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.Resource


@Service
class FieldSetGenerator {

    @Resource
    lateinit var dataMappingsService: DataMappingsService


    fun generateFieldSet(tableName: String): String {

        val name = dataMappingsService.getEntityDefaultNameByTableName(tableName)
        val mapping = dataMappingsService.getDataMapping(name)
        var s = """
                object ${name} : MFS<${name}>() {
                        val entity = "${name}"
                        val table = "${tableName}"
            """
        s+="\r\n"
        mapping.fields.values.forEach { it->
            s+= "                        ${buildField(it)}\r\n"
        }
        s += """
                }
            """
        return s
    }

    fun buildField(field: DataField):String
    {
        return when
        {
            field.javaType == Boolean::class.java -> "val ${field.name} = Field.boolean(\"${field.name}\")"
            field.javaType==String::class.java -> "val ${field.name} = Field.string(\"${field.name}\")"
            field.javaType==Long::class.java -> "val ${field.name} = Field.long(\"${field.name}\")"
            field.javaType==Int::class.java -> "val ${field.name} = Field.int(\"${field.name}\")"
            field.javaType== Date::class.java -> "val ${field.name} = Field.date(\"${field.name}\")"
            field.javaType==DataMap::class.java->"val ${field.name} = Field.reference(\"${field.name}\")"
            field.is1N->"val ${field.name} = Field.list(\"${field.name}\", ${field.oneToMany!!.to})"
            else ->""
        }
    }
}