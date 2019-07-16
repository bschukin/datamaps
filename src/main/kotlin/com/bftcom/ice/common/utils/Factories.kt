package com.bftcom.ice.common.utils

interface TThreadLocal<T:Any?>
{
    fun get(): T?
    fun set(builder: T)
    fun remove()
    fun getOrSet(default: () -> T): T
}