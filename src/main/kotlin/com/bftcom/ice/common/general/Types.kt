package com.bftcom.ice.common.general

import com.bftcom.ice.common.maps.TypedValue
import com.bftcom.ice.common.maps.ValuedEnum
import com.bftcom.ice.common.utils.*
import com.bftcom.ice.common.utils.Date
import com.bftcom.ice.common.utils.TypedArray
import com.bftcom.ice.common.utils.__infinity
import com.bftcom.ice.common.utils.infinity__
import java.util.*
import kotlin.reflect.KClass

enum class FieldType {
    Bool,
    String,
    Enum,
    Int,
    Long,
    Double,
    Date,
    Timestamp,
    Clob,
    Json,
    ByteArray,
    Guid,
    Array,
    Other;

    /**
     * Значение по умолчанию для текущего типа
     */
    val defaultValue: Any?
        get() = when (this) {
            Bool -> true
            String -> ""
            Enum -> null
            Int -> 0
            Long -> 0L
            Double -> 0.0
            Date -> Date()
            Timestamp -> Date()
            Clob -> null
            Json -> null
            ByteArray -> kotlin.ByteArray(0)
            Guid -> GUID()
            Array -> ArrayList<Any>(0)
            Other -> null
        }

    fun convertForTransport(value: Any?): Any? {
        if (value == null)
            return null

        return when (this) {
            String, Bool, Json -> value
            Int, Long, Double -> value.toString()
            Date -> {
                when (value) {
                    __infinity -> "__infinity"
                    infinity__ -> "infinity__"
                    else -> (value as com.bftcom.ice.common.utils.Date).getTime()
                }
            }
            Timestamp -> {
                (value as com.bftcom.ice.common.utils.Date).getTime()
            }
            Enum -> (value as ValuedEnum<*>).value
            Guid -> (value as GUID)._uuid
            Array -> {
                val values = value as? Iterable<Any> ?: (value as? kotlin.Array<*>)?.toList() ?: TODO()
                val valueType = values.firstOrNull()?.let { FieldType.getByValue(it) } ?: FieldType.Other
                val arrayValues = values.joinToString(",") { valueType.convertForTransport(it).toString() }

                TypedArray(valueType.name, arrayValues)
            }
            else -> TODO("Unexpected type for convert to transport: $this")
        }
    }

    fun extractFromTransport(value: Any?): Any? {
        if (value == null)
            return null

        return when (this) {
            String, Bool, Json -> value
            Double -> if (value is kotlin.Number) return value.toDouble() else (value as kotlin.String).toDouble()
            Long -> if (value is kotlin.Number) return value.toLong() else (value as kotlin.String).toLong()
            Int -> if (value is kotlin.Number) return value.toInt() else (value as kotlin.String).toInt()
            Date ->
                when (value) {
                    is Number -> Date(value)
                    is kotlin.String -> {
                        when (value) {
                            "infinity__" -> infinity__
                            "__infinity" -> __infinity
                            else -> value.toDate()
                        }
                    }
                    else -> TODO()
                }
            Timestamp -> value as? com.bftcom.ice.common.utils.Timestamp ?: (value as? Number)?.let { Timestamp(it) } ?: TODO()
            Enum -> value
            else -> getFieldValueByPlatformSpecialType(this, value)
        }
    }

    fun shouldExtractFromTransport(value: Any?): Boolean {
        if (value == null)
            return false

        return when (this) {
            String, Bool, ByteArray -> false
            Int, Long, Double -> true
            Date, Timestamp -> when (value) {
                is com.bftcom.ice.common.utils.Date -> false
                is Number -> true
                is kotlin.String -> true
                else -> TODO("Usupported date/timestamp type: $value")
            }
            Enum-> value is kotlin.Enum<*>
            Guid-> value is kotlin.String || value is Map<*,*>
            Array-> value is kotlin.String ||  value is Map<*,*>
            Other-> if(value is List<*>) false else TODO("Unsupported type for Other: $value")
            else -> TODO("Unsupported type: $this")
        }
    }

    fun castOrNull(value: Any?): Any? {
        return try {
            value?.let { castFromType(this.toString(), it) }
                ?.takeIf { it !is com.bftcom.ice.common.utils.Date || (it.getTime() != kotlin.Double.NaN) }
        } catch (_: Exception) {
            null
        }
    }

    val kotlinClass: KClass<out Any>
        get() {
            return when (this) {
                Bool -> Boolean::class
                String -> kotlin.String::class
                Enum -> kotlin.Enum::class
                Int -> kotlin.Int::class
                Long -> kotlin.Long::class
                Double -> kotlin.Double::class
                Date -> com.bftcom.ice.common.utils.Date::class
                Timestamp -> com.bftcom.ice.common.utils.Timestamp::class
                Clob -> CharSequence::class
                Json -> Map::class
                ByteArray -> kotlin.ByteArray::class
                Guid -> GUID::class
                Array -> kotlin.Array<Any>::class
                Other -> Any::class
            }
        }

    companion object {

        private var enableDebug = false

        fun getByValue(value: Any): FieldType {
            return when (value) {
                is kotlin.String -> String
                is kotlin.Boolean -> Bool
                is kotlin.Number -> when(value::class) {
                    kotlin.Short::class -> Int
                    kotlin.Int::class -> Int
                    kotlin.Long::class -> Long
                    kotlin.Float::class -> Double
                    kotlin.Double::class -> Double
                    else -> TODO("Type ${value::class} is not supported")
                }
                is com.bftcom.ice.common.utils.Timestamp -> Timestamp
                is com.bftcom.ice.common.utils.Date -> Date
                is kotlin.ByteArray -> ByteArray
            //при сериализации, TypedValue может десериализнуться как Map (например в случае ID имеющих тип Any)
                is Map<*, *> -> FieldType.valueOf(value[TypedValue::_type.name] as kotlin.String)
                else -> getFieldTypeByPlatformSpecialType(value)
            }
        }

        fun castFromType(typeName: kotlin.String, value: Any): Any {
            val fieldType = resolveTypeFromString(typeName)
            if (enableDebug) println("FieldType cast value:$value to $fieldType")
            return when (fieldType) {
                String -> cast(value, Caster(Any::class) { toString() })
                Bool -> cast(value, Caster(kotlin.String::class) { toBoolean() })
                Double -> cast(value, Caster(kotlin.String::class) { toDouble() }, Caster(kotlin.Number::class) { toDouble() })
                Int -> cast(value, Caster(kotlin.String::class) { toInt() }, Caster(kotlin.Number::class) { toInt() })
                Long -> cast(value, Caster(kotlin.String::class) { toLong() }, Caster(kotlin.Number::class) { toLong() })
                Timestamp, Date -> cast(value, Caster(kotlin.String::class) { toDate() }, Caster(kotlin.Number::class) { Date(this) })
                ByteArray -> cast<kotlin.ByteArray>(value)
                else -> getFieldValueByPlatformSpecialType(fieldType, value)
                    ?: throw UnsupportedOperationException("Cannot cast type of $typeName")
            }
        }

        private class Caster<T : Any, U : Any>(val klass: KClass<U>, val castFunction: U.() -> T?) {

            @Suppress("UNCHECKED_CAST")
            fun cast(value: Any): T? = if (klass.isInstance(value)) castFunction(value as U) else null
        }


        private inline fun <reified T : Any> cast(value: Any, vararg caster: Caster<T, *>): T {
            return (value as? T)
                ?: caster.map { it.cast(value) }.find { it != null }
                ?: throw IllegalStateException("Cannot cast $value as type of ${T::class}")
        }

        /**
         * Распознать тип по его строковому представлению из числа представленных типов
         */
        fun resolveTypeFromString(typeName: kotlin.String): FieldType {
            val lowerCase = typeName.toLowerCase().trim()
            if (enableDebug) println("Resolve type from string: $lowerCase")
            return when (lowerCase) {
                "int", "integer" -> Int
                "long" -> Long
                "double", "float" -> Double
                "boolean", "bool" -> Bool
                "date" -> Date
                "timestamp" -> Timestamp
                "guid" -> Guid
                "array", "list", "set" -> Array
                "enum", "const", "constant" -> Enum
                "string", "char", "charsequence" -> String
                "clob" -> Clob
                "json" -> Json
                "bytearray" -> ByteArray
                else -> Other
            }
        }
    }
}

fun getFieldTypeByPlatformSpecialType(value: Any):FieldType
{
    return when (value) {
        is java.sql.Timestamp -> FieldType.Timestamp
        is java.util.Date -> FieldType.Date
        is Enum<*> -> FieldType.Enum
        is GUID -> FieldType.Guid
        is UUID -> FieldType.Guid
        is MutableList<*> -> FieldType.Other
        else -> TODO()
    }
}

fun getFieldValueByPlatformSpecialType(fieldType: FieldType, value: Any): Any?
{
    return when (fieldType) {
        FieldType.Guid ->
            when (value) {
                is String -> UUID.fromString(value)
                is Map<*,*> -> return UUID.fromString(value["_value"] as String)
                else -> TODO()
            }
        FieldType.Array ->
            when (value) {
                is Map<*, *> -> {
                    val fieldTypeName = value[TypedArray::fieldType.name] as? String ?: FieldType.Other.name
                    val valueType = FieldType.valueOf(fieldTypeName)
                    val values  = value[TypedArray::values.name] as? String

                    values?.split(",")?.map { valueType.extractFromTransport(it) }?.toTypedArray()
                }
                is String ->  value.split(",").map { UUID.fromString(it) }.toTypedArray()
                else -> TODO()
            }
        else -> TODO()
    }
}