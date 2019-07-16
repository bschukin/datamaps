package com.bftcom.ice.datamaps.impl.util

import com.bftcom.ice.datamaps.misc.SomethingNotFound
import com.bftcom.ice.datamaps.ValuedEnum
import com.bftcom.ice.datamaps.misc.Date
import com.bftcom.ice.datamaps.misc.GUID
import com.bftcom.ice.datamaps.misc.Timestamp
import com.bftcom.ice.datamaps.impl.mappings.GenericDbMetadataService
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.support.ConversionServiceFactoryBean
import org.springframework.core.convert.converter.Converter
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.core.convert.support.GenericConversionService
import org.springframework.lang.Nullable
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.sql.Blob
import java.sql.Clob
import java.time.ZoneOffset
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.cast
import kotlin.reflect.full.isSuperclassOf

///для null - аргументов не кидается ошибкой для притимивных типов (что нужно для котлина)
///для ValuedEnum - возвращает value
@Component
class DefaultNullPassByConversionService : DefaultConversionService() {

    private val enums = mutableMapOf<String, Boolean>()

    override fun <T> convert(@Nullable source: Any?, targetType: Class<T>): T? {
        if (source == null)
            return source

        //todo: лучше бы разобраться и провзяать этот код как и остальные конвертеры
        if (isEnum(targetType)) {
            return extractEnum(targetType, source)
        }
        return super.convert(source, targetType)
    }

    companion object {
        private fun <T> extractEnum(targetType: Class<T>, source: Any?): T {
            val t = targetType.enumConstants.map { it as ValuedEnum<*> }.find {
                it.value == source
            }
            if (t != null)
                return t as T
            else
                throw SomethingNotFound("enum value for [$source] was not found")
        }
    }

    //рефлекшын по работе с енамами работает оооочень долго
    private fun isEnum(targetType: Class<*>): Boolean {
        val name = targetType.name
        var bool = enums[name]
        if (bool == null) {
            enums[name] = targetType.isEnum
            bool = enums[name]
        }
        return bool!!
    }
}

@Component
class ConvertHelper {

    @Autowired
    private lateinit var genericDbMetadataService: GenericDbMetadataService

    fun getJavaTypeToConvertToDb(obj: Any?): Class<*>? =
            when (obj) {
                is Timestamp -> java.sql.Timestamp::class.java
                is Date -> java.sql.Date::class.java
                is Enum<*> -> String::class.java
                is GUID, is UUID ->
                    when (genericDbMetadataService.isOracle() || genericDbMetadataService.isFirebird() ) {
                        true -> String::class.java //UUID не поддерживается в Oracle
                        false -> UUID::class.java
                    }
                is List<*> ->
                    if (obj.isNotEmpty()) {
                        getJavaTypeToConvertToDb(obj[0])
                    } else {
                        null
                    }
                is Array<*> -> PGobject::class.java.takeIf { genericDbMetadataService.isPostgreSql() }
                else -> null
            }
}

class NullPassByConversionServiceFactoryBean : ConversionServiceFactoryBean() {
    override fun createConversionService(): GenericConversionService {
        return DefaultNullPassByConversionService()
    }
}

internal class IceDateToDateConverter : Converter<Date, java.sql.Date?> {

    override fun convert(source: Date?): java.sql.Date? {
        if (source == null)
            return null
        return java.sql.Date(source.date.time)
    }
}

internal class IceTimestampToTimestampConverter : Converter<Timestamp, java.sql.Timestamp?> {

    override fun convert(source: Timestamp?): java.sql.Timestamp? {
        if (source == null)
            return null
        return java.sql.Timestamp(source.date.time)
    }
}

internal class SqlDateToIceDateConverter : Converter<java.sql.Date, Date> {

    override fun convert(source: java.sql.Date?): Date? {
        if (source == null)
            return null
        return Date(source)
    }
}

internal class OracleSqlTimestampToIceTimestampConverter : Converter<oracle.sql.TIMESTAMP, Timestamp> {

    override fun convert(source: oracle.sql.TIMESTAMP?): Timestamp? {
        if (source == null)
            return null
        return Timestamp(source.dateValue())
    }
}

internal class SqlTimestampToIceTimestampConverter : Converter<java.sql.Timestamp, Timestamp> {

    override fun convert(source: java.sql.Timestamp?): Timestamp? {
        if (source == null)
            return null
        return Timestamp(source)
    }
}

internal class NumberToDateConverter : Converter<Number, java.sql.Date?> {

    override fun convert(number: Number?): java.sql.Date? {
        return number?.let { java.sql.Date(it.toLong()) }
    }
}

internal class BigDecimalToBooleanConverter : Converter<BigDecimal, Boolean?> {

    override fun convert(number: BigDecimal?): Boolean? {
        return number?.let { number.signum() > 0 }
    }
}

internal class IntToBooleanConverter : Converter<Int, Boolean?> {

    override fun convert(number: Int?): Boolean? {
        return number?.let { number > 0 }
    }
}

internal class ClobToStringConverter : Converter<Clob, String?> {

    override fun convert(clob: Clob): String? {
        return clob.getSubString(1, clob.length().toInt())
    }
}

internal class BlobToByteArrayConverter : Converter<Blob, ByteArray?> {

    override fun convert(blob: Blob): ByteArray? {
        return blob.getBytes(1, blob.length().toInt())
    }
}

internal class PGobjectToStringConverter : Converter<PGobject, String> {

    override fun convert(pgobject: PGobject): String {
        return pgobject.value
    }
}

internal class GuidToUuidConverter : Converter<GUID, UUID> {

    override fun convert(pgobject: GUID): UUID {
        return UUID.fromString(pgobject._uuid)
    }
}

internal class GuidToStringConverter : Converter<GUID, String> {

    override fun convert(guid: GUID): String {
        return guid._uuid
    }
}

internal class UuidToGuidConverter : Converter<UUID, GUID> {

    override fun convert(pgobject: UUID): GUID {
        return GUID(pgobject.toString())
    }
}

internal class UuidArrayToPgObjectConverter : Converter<Array<UUID>, PGobject> {

    override fun convert(array: Array<UUID>): PGobject {
        val list = array.joinToString(",")
        val res = PGobject()
        res.type = "uuid[]"
        res.value = "{{$list}}"
        return res
    }
}

internal class StringArrayToPgObjectConverter : Converter<Array<String>, PGobject> {

    override fun convert(array: Array<String>): PGobject {
        val list = array.joinToString(",")
        val res = PGobject()
        res.type = "varchar[]"
        res.value = "{{$list}}"
        return res
    }
}

internal class ArrayToPgObjectConverter : Converter<Array<*>, PGobject> {

    override fun convert(array: Array<*>): PGobject {
        val list = array.joinToString(",")
        val res = PGobject()
        res.type = getPGObjectType(array.firstOrNull())
        res.value = "{{$list}}"
        return res
    }

    private fun getPGObjectType(value: Any?) = when(value) {
        is String -> "varchar[]"
        is UUID -> "uuid[]"
        is Number -> "numeric[]"
        is Boolean -> "bool[]"
        else -> "varchar[]"
    }
}

internal abstract class AbstractParameterConverter<T : Any> : Converter<Any, T> {
    abstract val target: KClass<T>

    final override fun convert(parameter: Any): T {
        val sourceClass = parameter::class.java.kotlin
        return when {
            target.isSuperclassOf(sourceClass) -> target.cast(parameter)
            else -> doConvert(parameter) ?: throw IllegalArgumentException("Cannot cast argument of $sourceClass to $target")
        }
    }

    protected abstract fun doConvert(parameter: Any): T?

}

internal class BooleanParameterConverter : AbstractParameterConverter<Boolean>() {
    override val target: KClass<Boolean> = Boolean::class

    override fun doConvert(parameter: Any): Boolean? =
        when (parameter) {
            is Boolean -> parameter
            is String -> parameter.toBoolean()
            else -> null
        }

    companion object {
        val booleanParameterConverter = BooleanParameterConverter()
    }
}

internal class CharParameterConverter : AbstractParameterConverter<Char>() {
    override val target: KClass<Char> = Char::class

    override fun doConvert(parameter: Any): Char? =
        when (parameter) {
            is Char -> parameter
            is String -> if (parameter.length != 1) throw IllegalArgumentException("Cannot cast String with length != 1 $parameter to Char type") else parameter[0]
            else -> null
        }

    companion object {
        val charParameterConverter = CharParameterConverter()
    }
}

internal class ShortParameterConverter : AbstractParameterConverter<Short>() {
    override val target: KClass<Short> = Short::class

    override fun doConvert(parameter: Any): Short? =
        when (parameter) {
            is Number -> parameter.toShort()
            is String -> parameter.toShort()
            is Char -> parameter.toString().toShort()
            else -> null
        }

    companion object {
        val shortParameterConverter = ShortParameterConverter()
    }
}

internal class ByteParameterConverter : AbstractParameterConverter<Byte>() {
    override val target: KClass<Byte> = Byte::class

    override fun doConvert(parameter: Any): Byte? =
        when (parameter) {
            is Number -> parameter.toByte()
            is String -> parameter.toByte()
            is Char -> parameter.toString().toByte()
            else -> null
        }

    companion object {
        val byteParameterConverter = ByteParameterConverter()
    }
}

internal class IntParameterConverter : AbstractParameterConverter<Int>() {
    override val target: KClass<Int> = Int::class

    override fun doConvert(parameter: Any): Int? =
        when (parameter) {
            is Number -> parameter.toInt()
            is String -> parameter.toInt()
            is Char -> parameter.toString().toInt()
            else -> null
        }

    companion object {
        val intParameterConverter = IntParameterConverter()
    }
}

internal class LongParameterConverter : AbstractParameterConverter<Long>() {
    override val target: KClass<Long> = Long::class

    override fun doConvert(parameter: Any): Long? =
        when (parameter) {
            is Number -> parameter.toLong()
            is String -> parameter.toLong()
            is Char -> parameter.toString().toLong()
            else -> null
        }

    companion object {
        val longParameterConverter = LongParameterConverter()
    }
}

internal class FloatParameterConverter : AbstractParameterConverter<Float>() {
    override val target: KClass<Float> = Float::class

    override fun doConvert(parameter: Any): Float? =
        when (parameter) {
            is Number -> parameter.toFloat()
            is String -> parameter.toFloat()
            is Char -> parameter.toString().toFloat()
            else -> null
        }

    companion object {
        val floatParameterConverter = FloatParameterConverter()
    }
}

internal class DoubleParameterConverter : AbstractParameterConverter<Double>() {
    override val target: KClass<Double> = Double::class

    override fun doConvert(parameter: Any): Double? =
        when (parameter) {
            is Number -> parameter.toDouble()
            is String -> parameter.toDouble()
            is Char -> parameter.toString().toDouble()
            else -> null
        }

    companion object {
        val doubleParameterConverter = DoubleParameterConverter()
    }
}

internal class NumberParameterConverter : AbstractParameterConverter<Number>() {
    override val target: KClass<Number> = Number::class

    override fun doConvert(parameter: Any): Number? =
        when (parameter) {
            is Char -> parameter.toString().toLong()
            is String -> if (parameter.contains(".")) parameter.toDouble() else parameter.toLong()
            else -> null
        }

    companion object {
        val numberParameterConverter = NumberParameterConverter()
    }
}

internal class PassParameterConverter : AbstractParameterConverter<Any>() {
    override val target: KClass<Any> = Any::class

    override fun doConvert(parameter: Any): Any? = parameter

    companion object {
        val passParameterConverter = PassParameterConverter()
    }
}

internal class DateParameterConverter : AbstractParameterConverter<Date>() {
    override val target: KClass<Date> = Date::class

    override fun doConvert(parameter: Any): Date? =
        when (parameter) {
            is Number -> Date(parameter)
            is java.util.Date -> Date(parameter)
            is java.time.LocalDate -> Date(parameter.toEpochDay())
            is java.time.LocalDateTime -> Date(parameter.toEpochSecond(ZoneOffset.UTC))
            is String -> try {
                Date(parameter.toLong())
            } catch (e: Exception) {
                null
            }
            is Char -> try {
                Date(parameter.toString().toLong())
            } catch (e: Exception) {
                null
            }
            else -> null
        }


    companion object {
        val dateParameterConverter = DateParameterConverter()
    }
}

/**
 * Преобразовать текущий массив элементов в подходящий массив параметров, если это возможно.
 */
infix fun Array<out Any?>.convertItems(parameters: Array<Class<out Any>>): Array<Any?> {
    if (size != parameters.size) {
        throw IllegalStateException("Cannot cast parameters because size of argument and parameters is not equals: " +
                "source size=$size, parameters size = ${parameters.size}")
    }
    return parameters
        .mapIndexed { index, parameter ->
            this[index]
                ?.let {
                    parameter.converter.convert(it)
                }
                ?: if (parameter.isPrimitive) {
                    throw IllegalStateException("Cannot cast null value to primitive")
                } else {
                    return@mapIndexed null
                }
        }.toTypedArray()
}

private inline val Class<*>.converter: Converter<Any, *>
    get() {
        return when (this) {
            Boolean::class.javaObjectType, Boolean::class.javaPrimitiveType -> BooleanParameterConverter.booleanParameterConverter
            Char::class.javaObjectType, Char::class.javaPrimitiveType -> CharParameterConverter.charParameterConverter
            Short::class.javaObjectType, Short::class.javaPrimitiveType -> ShortParameterConverter.shortParameterConverter
            Byte::class.javaObjectType, Byte::class.javaPrimitiveType -> ByteParameterConverter.byteParameterConverter
            Int::class.javaObjectType, Int::class.javaPrimitiveType -> IntParameterConverter.intParameterConverter
            Long::class.javaObjectType, Long::class.javaPrimitiveType -> LongParameterConverter.longParameterConverter
            Float::class.javaObjectType, Float::class.javaPrimitiveType -> FloatParameterConverter.floatParameterConverter
            Double::class.javaObjectType, Double::class.javaPrimitiveType -> DoubleParameterConverter.doubleParameterConverter
            Number::class.java -> NumberParameterConverter.numberParameterConverter
            Date::class.java -> DateParameterConverter.dateParameterConverter
            else -> PassParameterConverter.passParameterConverter
        }
    }