package com.bftcom.ice.common.utils

fun eraseAllWs(string: String): String = string.replace("\\s".toRegex(), "").replace("\"", "")

fun assertBodyEquals(string1: String, string2: String) {
    val s1 = eraseAllWs(string1).toLowerCase()
    val s2 = eraseAllWs(string2).toLowerCase()
    if(s1!=s2)
        throw RuntimeException("strings are not equal")
}