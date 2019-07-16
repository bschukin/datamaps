@file:Suppress("unused")

package com.bftcom.ice.server.util

import org.junit.Assert.*
import org.junit.Test
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.javaMethod

@Suppress("UNUSED_PARAMETER")
private class ReflectionTestClass {
    var intCallCount: Int = 0
    var numberCallCount: Int = 0
    var callWithoutArguments: Int = 0

    fun function() {
        callWithoutArguments++
    }

    /**
     * Function with int
     */
    fun function(int: Int){
        intCallCount++
    }

    /**
     * function with Number
     */
    fun function(number: Number) {
        numberCallCount++
    }

}


class ReflectionsKtTest {


    /**
     * Ожидается, что при передаче целого числа будет вызван метод принимающий int
     */
    @Test
    fun `test call method with int` () {
        val instance = ReflectionTestClass()
        ReflectionTestClass::class.java.findFunction("function", 1)
            ?.javaMethod?.invoke(instance, 1)
        assertEquals(1, instance.intCallCount)
    }

    /**
     * Ожидается, что будет вызван метод без аргументов
     */
    @Test
    fun `test call method without params int` () {
        val instance = ReflectionTestClass()
        ReflectionTestClass::class.java.findFunction("function")
            ?.javaMethod?.invoke(instance )
        assertEquals(1, instance.callWithoutArguments)
    }

    /**
     * Ожидается, что при передаче не целого числа будет вызван метод принимающий number. Т.е. произойдет кастование.
     */
    @Test
    fun `test call method with Double` () {
        val instance = ReflectionTestClass()
        val arg = 1.1
        ReflectionTestClass::class.java.findFunction("function", arg)
            ?.javaMethod?.invoke(instance, arg)
        assertEquals(1, instance.numberCallCount)
    }



    /**
     * Тест проверяет, как работает [kotlin.reflect.KClass.isSuperclassOf]
     *
     */
    @Test
    fun testSuperClass() {
        val primitiveType = Int::class.javaPrimitiveType
        assertEquals("Int::class.javaPrimitiveType must be primitive int", "int", primitiveType!!.name)
        val objectType = Int::class.javaObjectType
        assertEquals("Int::class.javaObjectType must be a java.lang.Integer", "java.lang.Integer", objectType.canonicalName)

        val intClass = primitiveType!!.kotlin
        assertTrue("int::class.kotlin must be equals kotlin.Int", Int::class == intClass)
        assertTrue("int::class.kotlin must be the same class kotlin.Int", Int::class === intClass)
        val integerClass = objectType.kotlin
        assertTrue("java.lang.Integer::class.kotlin must be equals Int::class", Int::class == integerClass)
        assertFalse("java.lang.Integer::class.kotlin must not be the same class Int::class", Int::class === integerClass)

        val numberClass = Number::class.java.kotlin
        assertNotEquals("Number::class.java.kotlin must not be equals ${Number::class.java.canonicalName}", Number::class.java, numberClass)

        assertTrue("Number must be a superclass of kotlin.Int!", numberClass.isSuperclassOf(intClass))
        assertTrue("kotlin.Int must be a superclass of itself!", intClass.isSuperclassOf(intClass))
        assertFalse("kotlin.Int must not be a superclass of java.lang.Number!", intClass.isSuperclassOf(numberClass))

        assertNotEquals("kotlin.Int must not be equals java.lang.Number", intClass, numberClass)
        assertFalse("kotlin.Int must not be instance of java.lang.Number", intClass.isInstance(numberClass))
    }

}