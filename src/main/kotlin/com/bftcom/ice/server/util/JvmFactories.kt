package com.bftcom.ice.common.utils

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