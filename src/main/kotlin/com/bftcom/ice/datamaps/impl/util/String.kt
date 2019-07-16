package com.bftcom.ice.datamaps.impl.util

import com.bftcom.ice.datamaps.DataMap


//взять св-во, получить св-во в виде строки, если null - пустая строка
fun getPropertyValueSafely(dynamicDM: DataMap, it: String) =
        if (dynamicDM[it] == null) "" else dynamicDM[it].toString()

fun Iterable<String?>.joinNoEmpty(delimeter: String = ", "): String {
    val buffer = StringBuffer()
    var count = 0
    for (element in this) {
        if (++count > 1 && !element.isNullOrEmpty() && buffer.isNotEmpty())
            buffer.append(delimeter)

        buffer.append(element)
    }
    return buffer.toString()
}