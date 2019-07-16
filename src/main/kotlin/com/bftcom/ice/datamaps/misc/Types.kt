package com.bftcom.ice.datamaps.misc

import com.bftcom.ice.datamaps.ValuedEnum
import com.bftcom.ice.datamaps.DeltaStore
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KClass

open class Date : Comparable<Date> {

    protected val calendar: Calendar

    constructor() {
        calendar = Calendar.getInstance()
    }

    constructor(date: java.util.Date) {
        calendar = Calendar.getInstance().apply {
            time = date
        }
    }

    constructor(year: Int, month: Int, day: Int) {
        calendar = GregorianCalendar(year, month-1, day)
    }

    constructor(milliseconds: Number) : this(java.util.Date(milliseconds.toLong()))

    val date: java.util.Date
        get() = calendar.time

    @JsonIgnore
    fun getDate() = calendar[Calendar.DAY_OF_MONTH]

    @JsonIgnore
    fun getMonth() = calendar[Calendar.MONTH]

    @JsonIgnore
    fun getFullYear() = calendar[Calendar.YEAR]

    fun minusDays(days:Int): Date {
        val cal: Calendar = calendar.clone() as Calendar
        cal.add(Calendar.DATE, -days)
        return Date(cal.getTime())
    }
    fun plusDays(days:Int): Date {
        val cal: Calendar = calendar.clone() as Calendar
        cal.add(Calendar.DATE, days)
        return Date(cal.getTime())
    }

    fun plusSeconds(seconds:Int): Date {
        val cal: Calendar = calendar.clone() as Calendar
        cal.add(Calendar.SECOND, seconds)
        return Date(cal.time)
    }


    @JsonProperty(COMMON_DATE_FIELD)
    open fun getTime(): Number = calendar.timeInMillis

    open fun toCommonDateString() = apiDateFormat.format(date)!!

    open fun toReadableString() = toReadableDateString()

    override fun equals(other: Any?): Boolean = other is Date && other.calendar.time == calendar.time

    override fun toString(): String {
        return "'${apiDateFormat.format(date)}'"
    }

    override fun compareTo(other: Date): Int {
        return this.getDate().compareTo(other.getDate())
    }
}

open class Timestamp : Date {

    constructor() : super()

    constructor(date: java.util.Date) : super(date)

    constructor(milliseconds: Number) : super(milliseconds)

    @JsonIgnore
    fun getHours() = calendar[Calendar.HOUR_OF_DAY]

    @JsonIgnore
    fun getMinutes() = calendar[Calendar.MINUTE]

    @JsonIgnore
    fun getSeconds() = calendar[Calendar.SECOND]

    @JsonProperty(COMMON_TIMESTAMP_FIELD)
    override fun getTime(): Number = calendar.timeInMillis

    override fun toCommonDateString() = apiDateFormat.format(date)!!

    override fun toReadableString() = toReadableTimeStampString()

}

const val COMMON_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
const val ISO_DATE_FORMAT = "yyyy-MM-dd"

val apiDateFormat = SimpleDateFormat(COMMON_DATE_FORMAT, Locale.US)
val izoDateFormat = SimpleDateFormat(ISO_DATE_FORMAT, Locale.US)

fun Date.toIsoDateString() = izoDateFormat.format(this.date)!!






const val COMMON_DATE_FIELD = "__dateMillis"

const val COMMON_TIMESTAMP_FIELD = "__timestampMillis"

operator fun Date.compareTo(otherDate: Date): Int = date.compareTo(otherDate.date)

fun parseDate(dateString: String): Date = Date(apiDateFormat.parse(dateString))

fun parseTimeStamp(dateString: String): Timestamp = Timestamp(apiDateFormat.parse(dateString))

fun String.toDate() = parseDate(this)

fun String.toTimeStamp() = parseTimeStamp(this)

fun Timestamp.toReadableTimeStampString(): String {
    return when {
        this == null -> ""
        this !is Date -> toString()
        this.getTime() == infinity__.getTime() -> "∞"
        this.getTime() == __infinity.getTime() -> "-∞"
        else -> "${toReadableDateString()} ${toReadableTimeString()}"
    }
}

fun Date.toReadableDateString(): String {
    return when {
        this == null -> ""
        this !is Date -> toString()
        this.getTime() == infinity__.getTime() -> "∞"
        this.getTime() == __infinity.getTime() -> "-∞"
        else -> "${zeroPadding(this.getDate())}.${zeroPadding(this.getMonth() + 1)}.${zeroPadding(this.getFullYear(), 4)}"
    }
}

fun Timestamp.toReadableTimeString(): String {
    return "${zeroPadding(this.getHours())}:${zeroPadding(this.getMinutes())}:${zeroPadding(this.getSeconds())}"
}

inline fun zeroPadding(int: Number, length: Int = 2) = int.toString().padStart(length, '0')


val __infinity= Date(-9223372036832400000) //значение постгресса 9.5
//плюс бесконечность
val infinity__= Date(9223372036825200000) //значение постгресса 9.5


val GUID_PATTERN = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

fun String.toBoolean() = this.equals("true", ignoreCase = true)

@Deprecated(message = "TODO удалить")
fun isTimestamp(number: Number) = (number.toString().length == 13)

fun isGuid(id: Any) = (id is String) && id.matches(GUID_PATTERN)

/*Класс для работы с UUID на стороне JS. */
class GUID {

    companion object {
        val constGUID = GUID("84479956-ba79-4e11-a8ff-7456acd4ab25")
    }

    val _uuid:String

    constructor(){
        _uuid = DeltaStore.newGuid()
    }

    constructor(s:String){
        _uuid = s
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GUID

        if (_uuid != other._uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return _uuid.hashCode()
    }

    override fun toString(): String {
        return _uuid
    }

}

data class TypedArray(val fieldType:String/*FieldType*/, val values:String)

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
                    else -> (value as com.bftcom.ice.datamaps.misc.Date).getTime()
                }
            }
            Timestamp -> {
                (value as com.bftcom.ice.datamaps.misc.Date).getTime()
            }
            Enum -> (value as ValuedEnum<*>).value
            Guid -> (value as GUID)._uuid
            Array -> {
                val values = value as? Iterable<Any> ?: (value as? kotlin.Array<*>)?.toList() ?: TODO()
                val valueType = values.firstOrNull()?.let { getByValue(it) } ?: Other
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
            Timestamp -> value as? com.bftcom.ice.datamaps.misc.Timestamp
                    ?: (value as? Number)?.let { Timestamp(it) } ?: TODO()
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
                is com.bftcom.ice.datamaps.misc.Date -> false
                is Number -> true
                is kotlin.String -> true
                else -> TODO("Usupported date/timestamp type: $value")
            }
            Enum -> value is kotlin.Enum<*>
            Guid -> value is kotlin.String || value is Map<*,*>
            Array -> value is kotlin.String ||  value is Map<*,*>
            Other -> if(value is List<*>) false else TODO("Unsupported type for Other: $value")
            else -> TODO("Unsupported type: $this")
        }
    }

    fun castOrNull(value: Any?): Any? {
        return try {
            value?.let { castFromType(this.toString(), it) }
                ?.takeIf { it !is com.bftcom.ice.datamaps.misc.Date || (it.getTime() != kotlin.Double.NaN) }
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
                Date -> com.bftcom.ice.datamaps.misc.Date::class
                Timestamp -> com.bftcom.ice.datamaps.misc.Timestamp::class
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
                is com.bftcom.ice.datamaps.misc.Timestamp -> Timestamp
                is com.bftcom.ice.datamaps.misc.Date -> Date
                is kotlin.ByteArray -> ByteArray
            //при сериализации, TypedValue может десериализнуться как Map (например в случае ID имеющих тип Any)
                is Map<*, *> -> TODO()
                else -> getFieldTypeByPlatformSpecialType(value)
            }
        }

        fun castFromType(typeName: kotlin.String, value: Any): Any {
            val fieldType = resolveTypeFromString(typeName)
            if (enableDebug) println("FieldType cast value:$value to $fieldType")
            return when (fieldType) {
                String -> cast(value, Caster(Any::class) { toString() })
                Bool -> cast(value, Caster(kotlin.String::class) { toBoolean() })
                Double -> cast(value, Caster(kotlin.String::class) { toDouble() }, Caster(Number::class) { toDouble() })
                Int -> cast(value, Caster(kotlin.String::class) { toInt() }, Caster(Number::class) { toInt() })
                Long -> cast(value, Caster(kotlin.String::class) { toLong() }, Caster(Number::class) { toLong() })
                Timestamp, Date -> cast(value, Caster(kotlin.String::class) { toDate() }, Caster(Number::class) { Date(this) })
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

fun getFieldTypeByPlatformSpecialType(value: Any): FieldType
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