package com.bftcom.ice.server.tools

import com.bftcom.ice.server.util.getLoadedClasses
import net.bytebuddy.ByteBuddy
import net.bytebuddy.agent.ByteBuddyAgent
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.net.URL
import java.net.URLClassLoader


/***
 * B[yte]B[uddy]Agent
 */
object BBAgent {

    init {
        ByteBuddyAgent.install()
    }

    fun refreshDataMapping(endOfName: String) {

        val c = getLoadedClasses().firstOrNull { f ->
            f.name.toUpperCase().endsWith(endOfName.toUpperCase())
        }
        if (c == null)
            println("not reloaded $endOfName")

        try {
            val pair = refreshClass(c!!)
            copyObjectInstances(pair.first, pair.second)
        } catch (e: Exception) {
            e.printStackTrace()
            println("not reloaded $endOfName")
            return
        }

        println("reloaded ${c.name}:{")
        c.declaredFields.filter(kotlinReflectionFilter()).forEach {
            it.isAccessible = true
            println("\t" + "${it.name}: ${it.get(c.kotlin.objectInstance)}")
        }
        println("}")
    }

    fun refreshClass(clazz: String) {
        Class.forName(clazz)
    }

    private fun refreshClass(clazz: Class<*>): Pair<Class<*>, Class<*>> {
        val dynClassLoader = getClassLoaderOnClassDir(clazz)
        try {
            val cl0 = dynClassLoader.loadClass(clazz.name)
            val builder = ByteBuddy()
                    .rebase(cl0)

            val cl = builder.make().load(BBAgent::class.java.classLoader,
                    ClassReloadingStrategy.fromInstalledAgent())
                    .loaded

            return Pair(cl0, cl)

        } catch (e: Throwable) {
            throw RuntimeException(e)
        }

    }


    private fun copyObjectInstances(cl0: Class<*>, cl1: Class<*>) {
        cl0.declaredFields.filter(kotlinReflectionFilter()).forEach {
            try {
                setFieldHard(it.name, cl0, cl1)
            } catch (e: Exception) {
                println(e)
            }
        }
    }


    private fun setFieldHard(name: String, cl0: Class<*>, cl1: Class<*>): Any {
        val obj0 = cl0.kotlin.objectInstance
        val f = cl0.getDeclaredField(name)
        f.isAccessible = true
        val value = f.get(obj0)

        val f1 = cl1.getDeclaredField(name)
        f1.isAccessible = true

        setFinalStatic1(f1, value, cl1.kotlin.objectInstance)
        return f1.get(cl1.kotlin.objectInstance)
    }

    private fun setFinalStatic1(field: Field, newValue: Any, objectInstance: Any?) {

        val modifiersField = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

        field.set(objectInstance, newValue)
    }

    private fun getClassLoaderOnClassDir(clazz: Class<*>): URLClassLoader {
        val urr = clazz.protectionDomain.codeSource.location
        return URLClassLoader2(clazz.name, arrayOf(urr))
    }

    private class URLClassLoader2(val className: String, urls: Array<URL>):
            URLClassLoader(urls, BBAgent.javaClass.classLoader) {

        @Throws(ClassNotFoundException::class)
        override fun loadClass(name: String, resolve: Boolean): Class<*> {

            var c: Class<*>? = null
            if (className == name) {
                synchronized(getClassLoadingLock(name)) {
                    // First, check if the class has already been loaded
                    c = findLoadedClass(name)
                    if (c == null) {
                        try {
                            c = findClass(name)
                            if (resolve) {
                                resolveClass(c)
                            }
                        } catch (e: ClassNotFoundException) {
                        }

                    }
                }
                if (c == null) {
                    c = super.loadClass(name, resolve)
                }
            } else
                c = parent.loadClass(name)


            return c!!
        }
    }



    private fun kotlinReflectionFilter(): (Field) -> Boolean = { it.name != "INSTANCE" && it.name != "Companion" }

}