package com.bftcom.ice.datamaps.common.utils


interface TThreadLocal<T:Any?>
{
    fun get(): T?
    fun set(builder: T)
    fun remove()
    fun getOrSet(default: () -> T): T
}

object ThreadLocalFactory
{
    fun <T> buildThreadLocal(): TThreadLocal<T> {
        val thw = ThreadLocalWrapper<T>()
        return thw
    }
}

class ThreadLocalWrapper<T> : TThreadLocal<T> {

    private val context = ThreadLocal<T>()


    override fun get(): T? {
        return context.get()
    }

    override fun set(builder: T) {
        context.set(builder)
    }

    override fun remove() {
        context.remove()
    }

    override fun getOrSet(default: () -> T): T {
        return get() ?: default().also(this::set)
    }

}