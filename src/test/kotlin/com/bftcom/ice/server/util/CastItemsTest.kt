package com.bftcom.ice.server.util

import com.bftcom.ice.common.general.throwImpossible
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.LocalDate
import java.util.*

@RunWith(org.junit.runners.Parameterized::class)
class CastItemsTest {

    @Suppress("unused", "PLATFORM_CLASS_MAPPED_TO_KOTLIN", "UNUSED_PARAMETER")
    inner class TestClass {
        fun applyNumber(int: Number) {}

        fun applyBooleanPrimitive(arg: Boolean) {}
        fun applyBoolean(arg: Boolean?) {}

        fun applyCharPrimitive(arg: Char) {}
        fun applyChar(arg: Char?) {}

        fun applyShortPrimitive(arg: Short) {}
        fun applyShort(arg: Short?) {}

        fun applyBytePrimitive(arg: Byte) {}
        fun applyByte(arg: Byte?) {}

        fun applyIntPrimitive(arg: Int) {}
        fun applyInt(arg: Int?) {}

        fun applyLongPrimitive(arg: Long) {}
        fun applyLong(arg: Long?) {}

        fun applyDoublePrimitive(arg: Double) {}
        fun applyDouble(arg: Double?) {}

        fun applyFloatPrimitive(arg: Float) {}
        fun applyFloat(arg: Float?) {}

        fun applyDate(arg: com.bftcom.ice.common.utils.Date) {}

        fun applyNullableAny(arg: Any?) {}
        fun applyAny(arg: Any) {}


    }

    private fun getTestClassMethod(methodName: String, vararg args: Class<out Any>?) =
        TestClass::class.java.getMethod(methodName, *args)

    @Parameterized.Parameter(value = 0)
    lateinit var caseName: String

    @Parameterized.Parameter(value = 1)
    lateinit var testData: Any

    @Test
    fun `test  castItems method`() {
        when (testData) {
            is TestData -> {
                val data = testData as TestData
                assertTrue(caseName, data.checker((arrayOf(data.source) convertItems arrayOf(data.argumentClass))[0]))
            }
            else -> throwImpossible()
        }
    }

    data class TestData(
        val source: Any?,
        val methodName: String,
        val argumentClass: Class<out Any>,
        val checker: (Any?) -> Boolean
    )

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun castItemTestData(): Array<Array<*>> {
            return arrayOf(
                arrayOf("boolean to boolean", TestData(true, "applyBooleanPrimitive", Boolean::class.javaPrimitiveType!!) { it == true }),
                arrayOf(""""true" to boolean""", TestData("true", "applyBooleanPrimitive", Boolean::class.javaPrimitiveType!!) { it == true }),
                arrayOf("boolean to Boolean", TestData(true, "applyBoolean", Boolean::class.javaObjectType) { it == true }),
                arrayOf(""""true" to Boolean""", TestData("true", "applyBoolean", Boolean::class.javaObjectType) { it == true }),
                arrayOf("null to Boolean", TestData(null, "applyBoolean", Boolean::class.javaObjectType) { it == null }),

                arrayOf("Char to char", TestData(1.toChar(), "applyCharPrimitive", Char::class.javaPrimitiveType!!) { it == 1.toChar() }),
                arrayOf(""""1" to char""", TestData("1", "applyCharPrimitive", Char::class.javaPrimitiveType!!) { it == '1' }),
                arrayOf("""'1' to char""", TestData('1', "applyCharPrimitive", Char::class.javaPrimitiveType!!) { it == '1' }),
                arrayOf("Char to Char", TestData(1.toChar(), "applyChar", Char::class.javaObjectType) { it == 1.toChar() }),
                arrayOf(""""1" to Char""", TestData("1", "applyChar", Char::class.javaObjectType) { it == '1' }),
                arrayOf("null to Char", TestData(null, "applyChar", Char::class.javaObjectType) { it == null }),


                arrayOf("Byte to byte", TestData(1.toByte(), "applyBytePrimitive", Byte::class.javaPrimitiveType!!) { it == 1.toByte() }),
                arrayOf("Short to byte", TestData(1.toShort(), "applyBytePrimitive", Byte::class.javaPrimitiveType!!) { it == 1.toByte() }),
                arrayOf("Int to byte", TestData(1, "applyBytePrimitive", Byte::class.javaPrimitiveType!!) { it == 1.toByte() }),
                arrayOf("Long to byte", TestData(1L, "applyBytePrimitive", Byte::class.javaPrimitiveType!!) { it == 1.toByte() }),
                arrayOf("Float to byte", TestData(1.0F, "applyBytePrimitive", Byte::class.javaPrimitiveType!!) { it == 1.toByte() }),
                arrayOf("Double to byte", TestData(1.0, "applyBytePrimitive", Byte::class.javaPrimitiveType!!) { it == 1.toByte() }),
                arrayOf("Number to byte", TestData(1 as Number, "applyBytePrimitive", Byte::class.javaPrimitiveType!!) { it == 1.toByte() }),
                arrayOf(""""1" to byte""", TestData("1", "applyBytePrimitive", Byte::class.javaPrimitiveType!!) { it == 1.toByte() }),
                arrayOf("""'1' to byte""", TestData('1', "applyBytePrimitive", Byte::class.javaPrimitiveType!!) { it == 1.toByte() }),

                arrayOf("Byte to Byte", TestData(1.toByte(), "applyByte", Byte::class.javaObjectType) { it == 1.toByte() }),
                arrayOf("Short to Byte", TestData(1.toShort(), "applyByte", Byte::class.javaObjectType) { it == 1.toByte() }),
                arrayOf("Int to Byte", TestData(1, "applyByte", Byte::class.javaObjectType) { it == 1.toByte() }),
                arrayOf("Long to Byte", TestData(1L, "applyByte", Byte::class.javaObjectType) { it == 1.toByte() }),
                arrayOf("Float to Byte", TestData(1.0F, "applyByte", Byte::class.javaObjectType) { it == 1.toByte() }),
                arrayOf("Double to Byte", TestData(1.0, "applyByte", Byte::class.javaObjectType) { it == 1.toByte() }),
                arrayOf("Number to Byte", TestData(1 as Number, "applyByte", Byte::class.javaObjectType) { it == 1.toByte() }),
                arrayOf(""""1" to Byte""", TestData("1", "applyByte", Byte::class.javaObjectType) { it == 1.toByte() }),
                arrayOf("""'1' to Byte""", TestData('1', "applyBytePrimitive", Byte::class.javaPrimitiveType!!) { it == 1.toByte() }),
                arrayOf("null to Byte", TestData(null, "applyByte", Byte::class.javaObjectType) { it == null }),

                arrayOf("Byte to short", TestData(1.toByte(), "applyShortPrimitive", Short::class.javaPrimitiveType!!) { it == 1.toShort() }),
                arrayOf("Short to short", TestData(1.toShort(), "applyShortPrimitive", Short::class.javaPrimitiveType!!) { it == 1.toShort() }),
                arrayOf("Int to short", TestData(1, "applyShortPrimitive", Short::class.javaPrimitiveType!!) { it == 1.toShort() }),
                arrayOf("Long to short", TestData(1L, "applyShortPrimitive", Short::class.javaPrimitiveType!!) { it == 1.toShort() }),
                arrayOf("Float to short", TestData(1.0F, "applyShortPrimitive", Short::class.javaPrimitiveType!!) { it == 1.toShort() }),
                arrayOf("Double to short", TestData(1.0, "applyShortPrimitive", Short::class.javaPrimitiveType!!) { it == 1.toShort() }),
                arrayOf("Number to short", TestData(1 as Number, "applyShortPrimitive", Short::class.javaPrimitiveType!!) { it == 1.toShort() }),
                arrayOf(""""1" to short""", TestData("1", "applyShortPrimitive", Short::class.javaPrimitiveType!!) { it == 1.toShort() }),
                arrayOf("""'1' to short""", TestData('1', "applyShortPrimitive", Short::class.javaPrimitiveType!!) { it == 1.toShort() }),

                arrayOf("Byte to Short", TestData(1.toByte(), "applyShort", Short::class.javaObjectType) { it == 1.toShort() }),
                arrayOf("Short to Short", TestData(1.toShort(), "applyShort", Short::class.javaObjectType) { it == 1.toShort() }),
                arrayOf("Int to Short", TestData(1, "applyShort", Short::class.javaObjectType) { it == 1.toShort() }),
                arrayOf("Long to Short", TestData(1L, "applyShort", Short::class.javaObjectType) { it == 1.toShort() }),
                arrayOf("Float to Short", TestData(1.0F, "applyShort", Short::class.javaObjectType) { it == 1.toShort() }),
                arrayOf("Double to Short", TestData(1.0, "applyShort", Short::class.javaObjectType) { it == 1.toShort() }),
                arrayOf("Number to Short", TestData(1 as Number, "applyShort", Short::class.javaObjectType) { it == 1.toShort() }),
                arrayOf(""""1" to Short""", TestData("1", "applyShort", Short::class.javaObjectType) { it == 1.toShort() }),
                arrayOf("""'1' to Short""", TestData('1', "applyShort", Short::class.javaObjectType) { it == 1.toShort() }),
                arrayOf("null to Short", TestData(null, "applyShort", Short::class.javaObjectType) { it == null }),

                arrayOf("Byte to int", TestData(1.toByte(), "applyIntPrimitive", Int::class.javaPrimitiveType!!) { it == 1 }),
                arrayOf("Short to int", TestData(1.toShort(), "applyIntPrimitive", Int::class.javaPrimitiveType!!) { it == 1 }),
                arrayOf("Int to int", TestData(1, "applyIntPrimitive", Int::class.javaPrimitiveType!!) { it == 1 }),
                arrayOf("Long to int", TestData(1L, "applyIntPrimitive", Int::class.javaPrimitiveType!!) { it == 1 }),
                arrayOf("Float to int", TestData(1.0F, "applyIntPrimitive", Int::class.javaPrimitiveType!!) { it == 1 }),
                arrayOf("Double to Iit", TestData(1.0, "applyIntPrimitive", Int::class.javaPrimitiveType!!) { it == 1 }),
                arrayOf("Number to int", TestData(1 as Number, "applyIntPrimitive", Int::class.javaPrimitiveType!!) { it == 1 }),
                arrayOf(""""1" to int""", TestData("1", "applyIntPrimitive", Int::class.javaPrimitiveType!!) { it == 1 }),
                arrayOf("""'1' to int""", TestData('1', "applyIntPrimitive", Int::class.javaPrimitiveType!!) { it == 1 }),

                arrayOf("Byte to Int", TestData(1.toByte(), "applyInt", Int::class.javaObjectType) { it == 1 }),
                arrayOf("Short to Int", TestData(1.toShort(), "applyInt", Int::class.javaObjectType) { it == 1 }),
                arrayOf("Int to Int", TestData(1, "applyInt", Int::class.javaObjectType) { it == 1 }),
                arrayOf("Long to Int", TestData(1L, "applyInt", Int::class.javaObjectType) { it == 1 }),
                arrayOf("Float to Int", TestData(1.0F, "applyInt", Int::class.javaObjectType) { it == 1 }),
                arrayOf("Double to Int", TestData(1.0, "applyInt", Int::class.javaObjectType) { it == 1 }),
                arrayOf("Number to Int", TestData(1 as Number, "applyInt", Int::class.javaObjectType) { it == 1 }),
                arrayOf(""""1" to Int""", TestData("1", "applyInt", Int::class.javaObjectType) { it == 1 }),
                arrayOf("""'1' to Int""", TestData('1', "applyInt", Int::class.javaObjectType) { it == 1 }),
                arrayOf("null to Int", TestData(null, "applyInt", Int::class.javaObjectType) { it == null }),

                arrayOf("Byte to long", TestData(1.toByte(), "applyLongPrimitive", Long::class.javaPrimitiveType!!) { it == 1L }),
                arrayOf("Short to long", TestData(1.toShort(), "applyLongPrimitive", Long::class.javaPrimitiveType!!) { it == 1L }),
                arrayOf("Int to long", TestData(1, "applyLongPrimitive", Long::class.javaPrimitiveType!!) { it == 1L }),
                arrayOf("Long to long", TestData(1L, "applyLongPrimitive", Long::class.javaPrimitiveType!!) { it == 1L }),
                arrayOf("Float to long", TestData(1.0F, "applyLongPrimitive", Long::class.javaPrimitiveType!!) { it == 1L }),
                arrayOf("Double to long", TestData(1.0, "applyLongPrimitive", Long::class.javaPrimitiveType!!) { it == 1L }),
                arrayOf("Number to long", TestData(1 as Number, "applyLongPrimitive", Long::class.javaPrimitiveType!!) { it == 1L }),
                arrayOf(""""1" to long""", TestData("1", "applyLongPrimitive", Long::class.javaPrimitiveType!!) { it == 1L }),
                arrayOf("""'1' to long""", TestData('1', "applyLongPrimitive", Long::class.javaPrimitiveType!!) { it == 1L }),

                arrayOf("Byte to Long", TestData(1.toByte(), "applyLong", Long::class.javaObjectType) { it == 1L }),
                arrayOf("Short to Long", TestData(1.toShort(), "applyLong", Long::class.javaObjectType) { it == 1L }),
                arrayOf("Int to Long", TestData(1, "applyLong", Long::class.javaObjectType) { it == 1L }),
                arrayOf("Long to Long", TestData(1L, "applyLong", Long::class.javaObjectType) { it == 1L }),
                arrayOf("Float to Long", TestData(1.0F, "applyLong", Long::class.javaObjectType) { it == 1L }),
                arrayOf("Double to Long", TestData(1.0, "applyLong", Long::class.javaObjectType) { it == 1L }),
                arrayOf("Number to Long", TestData(1 as Number, "applyLong", Long::class.javaObjectType) { it == 1L }),
                arrayOf(""""1" to Long""", TestData("1", "applyLong", Long::class.javaObjectType) { it == 1L }),
                arrayOf("""'1' to Long""", TestData('1', "applyLong", Long::class.javaObjectType) { it == 1L }),
                arrayOf("null to Long", TestData(null, "applyLong", Long::class.javaObjectType) { it == null }),

                arrayOf("Byte to float", TestData(1.toByte(), "applyFloatPrimitive", Float::class.javaPrimitiveType!!) { it == 1.0F }),
                arrayOf("Short to float", TestData(1.toShort(), "applyFloatPrimitive", Float::class.javaPrimitiveType!!) { it == 1.0F }),
                arrayOf("Int to float", TestData(1, "applyFloatPrimitive", Float::class.javaPrimitiveType!!) { it == 1.0F }),
                arrayOf("Float to float", TestData(1L, "applyFloatPrimitive", Float::class.javaPrimitiveType!!) { it == 1.0F }),
                arrayOf("Float to float", TestData(1.0F, "applyFloatPrimitive", Float::class.javaPrimitiveType!!) { it == 1.0F }),
                arrayOf("Double to float", TestData(1.0, "applyFloatPrimitive", Float::class.javaPrimitiveType!!) { it == 1.0F }),
                arrayOf("Number to float", TestData(1 as Number, "applyFloatPrimitive", Float::class.javaPrimitiveType!!) { it == 1.0F }),
                arrayOf(""""1" to float""", TestData("1", "applyFloatPrimitive", Float::class.javaPrimitiveType!!) { it == 1.0F }),
                arrayOf("""'1' to float""", TestData('1', "applyFloatPrimitive", Float::class.javaPrimitiveType!!) { it == 1.0F }),

                arrayOf("Byte to Float", TestData(1.toByte(), "applyFloat", Float::class.javaObjectType) { it == 1.0F }),
                arrayOf("Short to Float", TestData(1.toShort(), "applyFloat", Float::class.javaObjectType) { it == 1.0F }),
                arrayOf("Int to Float", TestData(1, "applyFloat", Float::class.javaObjectType) { it == 1.0F }),
                arrayOf("Float to Float", TestData(1L, "applyFloat", Float::class.javaObjectType) { it == 1.0F }),
                arrayOf("Float to Float", TestData(1.0F, "applyFloat", Float::class.javaObjectType) { it == 1.0F }),
                arrayOf("Double to Float", TestData(1.0, "applyFloat", Float::class.javaObjectType) { it == 1.0F }),
                arrayOf("Number to Float", TestData(1 as Number, "applyFloat", Float::class.javaObjectType) { it == 1.0F }),
                arrayOf(""""1" to Float""", TestData("1", "applyFloat", Float::class.javaObjectType) { it == 1.0F }),
                arrayOf("""'1' to Float""", TestData('1', "applyFloat", Float::class.javaObjectType) { it == 1.0F }),
                arrayOf("null to Float", TestData(null, "applyFloat", Float::class.javaObjectType) { it == null }),

                arrayOf("Byte to double", TestData(1.toByte(), "applyDoublePrimitive", Double::class.javaPrimitiveType!!) { it == 1.0 }),
                arrayOf("Short to double", TestData(1.toShort(), "applyDoublePrimitive", Double::class.javaPrimitiveType!!) { it == 1.0 }),
                arrayOf("Int to double", TestData(1, "applyDoublePrimitive", Double::class.javaPrimitiveType!!) { it == 1.0 }),
                arrayOf("Double to double", TestData(1L, "applyDoublePrimitive", Double::class.javaPrimitiveType!!) { it == 1.0 }),
                arrayOf("Double to double", TestData(1.0F, "applyDoublePrimitive", Double::class.javaPrimitiveType!!) { it == 1.0 }),
                arrayOf("Double to double", TestData(1.0, "applyDoublePrimitive", Double::class.javaPrimitiveType!!) { it == 1.0 }),
                arrayOf("Number to double", TestData(1 as Number, "applyDoublePrimitive", Double::class.javaPrimitiveType!!) { it == 1.0 }),
                arrayOf(""""1.0" to double""", TestData("1.0", "applyDoublePrimitive", Double::class.javaPrimitiveType!!) { it == 1.0 }),
                arrayOf(""""1" to double""", TestData("1", "applyDoublePrimitive", Double::class.javaPrimitiveType!!) { it == 1.0 }),
                arrayOf("""'1' to double""", TestData('1', "applyDoublePrimitive", Double::class.javaPrimitiveType!!) { it == 1.0 }),

                arrayOf("Byte to Double", TestData(1.toByte(), "applyDouble", Double::class.javaObjectType) { it == 1.0 }),
                arrayOf("Short to Double", TestData(1.toShort(), "applyDouble", Double::class.javaObjectType) { it == 1.0 }),
                arrayOf("Int to Double", TestData(1, "applyDouble", Double::class.javaObjectType) { it == 1.0 }),
                arrayOf("Double to Double", TestData(1L, "applyDouble", Double::class.javaObjectType) { it == 1.0 }),
                arrayOf("Double to Double", TestData(1.0F, "applyDouble", Double::class.javaObjectType) { it == 1.0 }),
                arrayOf("Double to Double", TestData(1.0, "applyDouble", Double::class.javaObjectType) { it == 1.0 }),
                arrayOf("Number to Double", TestData(1 as Number, "applyDouble", Double::class.javaObjectType) { it == 1.0 }),
                arrayOf(""""1" to Double""", TestData("1", "applyDouble", Double::class.javaObjectType) { it == 1.0 }),
                arrayOf("""'1' to Double""", TestData('1', "applyDouble", Double::class.javaObjectType) { it == 1.0 }),
                arrayOf("null to Double", TestData(null, "applyDouble", Double::class.javaObjectType) { it == null }),

                arrayOf("Byte to Number", TestData(1.toByte(), "applyNumber", Number::class.javaObjectType) { it == 1.toByte() }),
                arrayOf("Short to Number", TestData(1.toShort(), "applyNumber", Number::class.javaObjectType) { it == 1.toShort() }),
                arrayOf("Int to Number", TestData(1, "applyNumber", Number::class.javaObjectType) { it == 1 }),
                arrayOf("Number to Number", TestData(1L, "applyNumber", Number::class.javaObjectType) { it == 1L }),
                arrayOf("Number to Number", TestData(1.0F, "applyNumber", Number::class.javaObjectType) { it == 1.0F }),
                arrayOf("Number to Number", TestData(1.0, "applyNumber", Number::class.javaObjectType) { it == 1.0 }),
                arrayOf("Number to Number", TestData(1 as Number, "applyNumber", Number::class.javaObjectType) { it == 1 }),
                arrayOf(""""1" to Number""", TestData("1", "applyNumber", Number::class.javaObjectType) { it == 1L }),
                arrayOf("""'1' to Number""", TestData('1', "applyNumber", Number::class.javaObjectType) { it == 1L }),
                arrayOf("null to Number", TestData(null, "applyNumber", Number::class.javaObjectType) { it == null }),

                arrayOf("java.util.Date to com.bftcom.ice.common.utils.Date",
                    TestData(Date(), "applyDate", com.bftcom.ice.common.utils.Date::class.java) { it is com.bftcom.ice.common.utils.Date }),
                arrayOf(
                    "java.time.LocalDate to com.bftcom.ice.common.utils.Date",
                    TestData(LocalDate.now(), "applyDate", com.bftcom.ice.common.utils.Date::class.java) { it is com.bftcom.ice.common.utils.Date }),
                arrayOf(""""1" to  com.bftcom.ice.common.utils.Date""",
                    TestData("1", "applyDate", com.bftcom.ice.common.utils.Date::class.java) { it is com.bftcom.ice.common.utils.Date }),
                arrayOf("""'1' to  com.bftcom.ice.common.utils.Date""",
                    TestData('1', "applyDate", com.bftcom.ice.common.utils.Date::class.java) { it is com.bftcom.ice.common.utils.Date }),
                arrayOf("""1000 to  com.bftcom.ice.common.utils.Date""",
                    TestData(1000, "applyDate", com.bftcom.ice.common.utils.Date::class.java) { it is com.bftcom.ice.common.utils.Date }),
                arrayOf("null to  com.bftcom.ice.common.utils.Date", TestData(null, "applyDate", com.bftcom.ice.common.utils.Date::class.java) { it == null }),


                arrayOf("null to Any?", TestData(null, "applyNullableAny", Any::class.java) { it == null }),
                arrayOf("java.util.Date to Any", TestData(Date(), "applyAny", Any::class.java) { it is Any })

            )

        }
    }
}