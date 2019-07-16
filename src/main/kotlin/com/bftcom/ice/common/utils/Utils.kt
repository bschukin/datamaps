package com.bftcom.ice.common.utils

import com.bftcom.ice.common.maps.DeltaStore

val GUID_PATTERN = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")

fun String.toBoolean() = this.equals("true", ignoreCase = true)

@Deprecated(message = "TODO удалить")
fun isTimestamp(number: Number) = (number.toString().length == 13)

fun isGuid(id: Any) = (id is String) && id.matches(GUID_PATTERN)

/*Класс для работы с UUID на стороне JS. */
class GUID {

    companion object {
        val constGUID = GUID("84479956-ba79-4e11-a8ff-7456acd4ab25")
    }

    val _uuid:String

    constructor(){
        _uuid = DeltaStore.newGuid()
    }

    constructor(s:String){
        _uuid = s
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GUID

        if (_uuid != other._uuid) return false

        return true
    }

    override fun hashCode(): Int {
        return _uuid.hashCode()
    }

    override fun toString(): String {
        return _uuid
    }

}

data class TypedArray(val fieldType:String/*FieldType*/, val values:String)
