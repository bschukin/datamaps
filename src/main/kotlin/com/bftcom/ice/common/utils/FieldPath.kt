package com.bftcom.ice.common.utils

import com.bftcom.ice.common.maps.Field

class FieldPath(val fields: List<String>): Iterable<String> {

    constructor(vararg fields: String?) : this(fields.filterNotNull().flatMap { it.split(".") })

    constructor(vararg fields: Field<*, *>?) : this(fields.filterNotNull().map { it.n })

    override fun iterator(): Iterator<String> {
        return fields.iterator()
    }

    operator fun plus(path: FieldPath): FieldPath {
        return FieldPath(this.fields + path.fields)
    }

    operator fun plus(path: String?): FieldPath {
        return this + FieldPath(path)
    }

    fun isEmpty(): Boolean {
        return fields.isEmpty()
    }

    fun isNotEmpty(): Boolean {
        return fields.isNotEmpty()
    }

    fun first(): String? {
        return fields.firstOrNull()
    }

    fun last(): String? {
        return fields.lastOrNull()
    }

    fun parent(): FieldPath? {
        return if (fields.size > 1) FieldPath(fields.subList(0, fields.size - 1)) else null
    }

    fun subpath(fromIndex: Int? = null, toIndex: Int? = null): FieldPath? {
        return FieldPath(fields.subList(fromIndex ?: 0, toIndex ?: fields.size))
    }

    val n: String get() = toString()

    override fun toString(): String {
        return fields.joinToString(".")
    }

    override fun equals(other: Any?): Boolean {
        return (other as? FieldPath)?.fields?.equals(this.fields) == true
    }

    override fun hashCode(): Int {
        return fields.hashCode()
    }
}

fun fieldPath(vararg fields: Field<*, *>?) = FieldPath(*fields)

fun fieldPath(vararg fields: String?) = FieldPath(*fields)