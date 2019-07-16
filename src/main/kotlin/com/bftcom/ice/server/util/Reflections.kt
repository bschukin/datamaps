package com.bftcom.ice.server.util

import com.bftcom.ice.common.maps.DataMapF
import java.lang.reflect.Method
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.valueParameters


fun findMethodByArguments(clazz: Class<*>, name: String, args: Array<Any?>): Method? {
    val types = getArgTypes(args)
    return findMethod(clazz, name, types)
}

private fun getArgTypes(arguments: Array<Any?>): Array<Class<*>> {
    if (arguments == null) {
        return arrayOf()
    }
    val argTypes = arrayOfNulls<Class<*>>(arguments.size)
    for (i in arguments.indices) {
        argTypes[i] = if (arguments[i] == null) null else arguments[i]!!.javaClass
    }

    return argTypes as Array<Class<*>>
}

/**
 * Метод, украденный в org.springframework.util.ReflectionUtils,
 * но умеющий искать объекты не по только по точному совпадению типа аргументов...
 *
 *
 * Attempt to find a [Method] on the supplied class with the supplied name
 * and parameter types. Searches all superclasses up to `Object`.
 *
 * Returns `null` if no [Method] can be found.
 *
 * @param clazz      the class to introspect
 * @param name       the name of the method
 * @param paramTypes the parameter types of the method
 * @return the Method object, or `null` if none found
 */
fun findMethod(clazz: Class<*>, name: String, paramTypes: Array<Class<*>>): Method? {
    var searchType: Class<*>? = clazz
    while (Object::class.java != searchType && searchType != null) {
        val methods = if (searchType.isInterface) searchType.methods else searchType.declaredMethods
        for (i in methods.indices) {
            val method = methods[i]
            if (name == method.name
                    //основное отличие от спринговых утилит - вот
                    && isClassesAssignable(method.parameterTypes, paramTypes)) {
                return method
            }
        }
        searchType = searchType.superclass
    }
    return null
}

fun isClassesAssignable(defined: Array<Class<*>>?, args: Array<Class<*>>?): Boolean {
    if (defined == args)
        return true
    if (defined == null || args == null)
        return false

    val length = defined.size
    if (args.size != length)
        return false

    //пока, как и всегда, по простому
    /*for (i in 0 until length) {
        val d1 = defined[i]
        val arg1 = args[i]
        if (!(if (d1 == null) arg1 == null else arg1 == null || d1.isAssignableFrom(arg1)))
            return false
    }*/

    return true
}
fun findClassByName(possiblySimpleName: String): Class<out Any>? {
    //1 пытаемся грузануть по имени класса
    try {
        return Class.forName(possiblySimpleName) as Class<out Any>
    } catch (e: ClassNotFoundException) {
    }
    //2 ищем по частичному совпадению
    return getLoadedClasses().find { it.simpleName.equals(possiblySimpleName, true) }
}

fun getLoadedClasses(): List<Class<out Any>> {
    val f = ClassLoader::class.java.getDeclaredField("classes")
    f.isAccessible = true

    val classLoader = DataMapF::class.java.classLoader
    return (f.get(classLoader) as Vector<Class<Any>>).toList()

}

/**
 * Attempt to find a [KFunction] on this class with the supplied functionName
 * and [paramTypes]. Searches all superclasses up to `Object`.
 *
 * NOTE!!! There is not difference between primitive and covers and superclasses.
 * If there are some methods with the same name and they are have signature differed only primitive, covers,
 * or class inheritance which one will be returned is unknown
 *
 * Returns `null` if no [KFunction] can be found.
 */
fun Class<*>.findFunction(functionName: String, vararg paramTypes: Any?): KFunction<*>? =
    kotlin
        .declaredFunctions
        .filter {
            it.name == functionName && it.valueParameters.size == paramTypes.size
        }
        .find { function ->
            function.valueParameters
                .asSequence()
                .zip(paramTypes.asSequence())
                .all { pair ->
                    val parameter = pair.first
                    val parameterType = parameter.type
                    val argument = pair.second
                    val classifier = parameterType.classifier
                    return@all when {
                        // Нельзя вызывать если помеченно isMarkedNullable.not() и аргумент == null
                        // За некоторыми исключениями. Смотри документацию.
                        parameterType.isMarkedNullable.not() && argument == null -> false
                        // Если argument == null и прошли проверку выше, значит можно вызывать с null
                        argument == null -> true
                        // Теперь проверим тип
                        // classifier.isSuperclassOf(argument.kotlin) вернет true при проверке
                        // int & Int , Int & int, Number & int, Number & Int, т.е. есть возможность кастовать.
                        // На это есть тест
                        classifier is KClass<*> -> classifier.isSuperclassOf(argument::class)
                        // Упс, кажется мы не можем вызвать метод с такими параметрами
                        else -> false
                    }
                }
        }