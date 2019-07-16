package com.bftcom.ice.common.utils

import com.bftcom.ice.common.general.FieldType
import com.bftcom.ice.common.maps.*

/**
 * Филдсет метаописания функции для передачи на UI в компонент выбора сервиса
 */
object ServiceMetaInfo : MappingFieldSet<ServiceMetaInfo>("ServiceMetaInfo", Transience, Dynamic) {
    val name = Field.stringNN("name")
    val packageName = Field.stringNN("packageName")
    val functions = Field.list("functions", FunMetaInfo)
}

object FunParamMetaInfo : MappingFieldSet<FunParamMetaInfo>("FunParamMetaInfo", Transience, Dynamic) {
    val type = Field.stringNN("type")
    val parameterName = Field.string("parameterName")
    val parameterType = Field.string("parameterType")
    val parameterDescription = Field.string("parameterDescription")
    val parameterConstraint = Field.string("parameterConstraint")
    val regex = Field.string("regex")
}

fun DataMapF<ServiceMetaInfo>.fullName(): String {
    return "${this[ServiceMetaInfo.packageName]}.${this[ServiceMetaInfo.name]}"
}

object FunMetaInfo : MappingFieldSet<FunMetaInfo>("FunMetaInfo", Transience, Dynamic) {
    val name = Field.stringNN("name")
    val serviceName = Field.stringNN("serviceName")
    val params = Field.list("params", FunParamMetaInfo)
}

fun DataMapF<FunMetaInfo>.functionFullName(): String {
    return "${this[FunMetaInfo.serviceName]}:${this.functionNameWithArgs()}"
}

fun DataMapF<FunMetaInfo>.functionName(): String {
    return "${this[FunMetaInfo.serviceName]}:${this[FunMetaInfo.name]}"
}

fun DataMapF<FunMetaInfo>.functionNameWithArgs(): String {
    val params = this[FunMetaInfo.params].joinToString { FieldType.resolveTypeFromString(it[FunParamMetaInfo.type]).toString() }
    return "${this[FunMetaInfo.name]}($params)"
}


