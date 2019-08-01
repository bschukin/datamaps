package com.bftcom.ice.datamaps.misc

fun <V> linkedCaseInsMapOf(): OrderedCaseInsensitiveMap<V> = OrderedCaseInsensitiveMap()
fun <V> linkedCaseInsMapOf(vararg pairs: Pair<String, V>): OrderedCaseInsensitiveMap<V> =
        if (pairs.isNotEmpty()) pairs.toMap(OrderedCaseInsensitiveMap()) else linkedCaseInsMapOf()

open class CaseInsensitiveKeyMap<V>(private val map: MutableMap<Key, V> = mutableMapOf()) : MutableMap<String, V> {

    companion object {
        fun <VV> create(toCopy: Map<String, VV>): CaseInsensitiveKeyMap<VV> {
            val res = CaseInsensitiveKeyMap<VV>()
            toCopy.forEach {
                res[it.key] = it.value
            }
            return res
        }

    }

    fun toReadonly(): CaseInsensitiveKeyMap<V>
    {
        return ReadonlyCaseInsensitiveKeyMap(this)
    }

    override fun containsValue(value: V): Boolean {
        return map.containsValue(value)
    }

    override fun isEmpty(): Boolean {
        return map.isEmpty()
    }

    override fun clear() {
        return map.clear()
    }

    override val size: Int
        get() = map.size

    override fun containsKey(key: String): Boolean {
        return this.map.containsKey(convertKey(key))
    }

    override fun get(key: String): V? {
        return this.map[convertKey(key)]
    }

    override fun remove(key: String): V? {
        return this.map.remove(convertKey(key))
    }

    override fun put(key: String, value: V): V? {
        val k = convertKey(key)
        val old = this.map.remove(k)
        this.map[k] = value
        return old
    }

    override fun putAll(from: Map<out String, V>) {
        for ((key, value) in from) {
            this[key] = value
        }
    }

    open fun computeIfAbsent(key: String,
                             mappingFunction: (String) -> V): V {
        val v = get(key)
        if (v == null) {
            val newValue: V? = mappingFunction(key)
            if (newValue != null) {
                put(key, newValue)
                return newValue
            }
        }
        return v!!
    }

    private fun convertKey(key: String): Key {
        return Key(key)
    }

    override val keys: MutableSet<String>
        get() = map.keys.map { key -> key.key }.toMutableSet()

    /**
     * Returns a [MutableSet] of all key/value pairs in this map.
     */
    override val entries: MutableSet<MutableMap.MutableEntry<String, V>>
        get() = map.entries.map { e -> MEntry(e.key.key, e.value) }.toMutableSet()

    override val values: MutableCollection<V>
        get() = map.values

    class MEntry<V>(override val key: String, override val value: V) :
            MutableMap.MutableEntry<String, V> {
        override fun setValue(newValue: V): V {
            TODO()
        }

    }

    class Key(val key: String) {
        private val lcKey: String = key.toLowerCase()

        override fun hashCode(): Int {
            return this.lcKey.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            } else if (other== null) {
                return false
            }
            val otherKey = other as Key?
            return this.lcKey == otherKey?.lcKey
        }

    }

    override fun toString(): String = "orci-map{" + this.map { e -> entryToString(e) }
            .joinToString(";") + "}"

    private fun entryToString(e: Map.Entry<*, *>): String = with(e) { "$key=$value" }

    override fun equals(other: Any?): Boolean {
        val other2 = if (other is CaseInsensitiveKeyMap<*>) other.toMap() else other
        return this.toMap() == other2
    }

    override fun hashCode(): Int {
        return map.hashCode()
    }

    class ReadonlyCaseInsensitiveKeyMap<V>(target: CaseInsensitiveKeyMap<V>) : CaseInsensitiveKeyMap<V>() {

        init {
            target.forEach {
                super.put(it.key, it.value)
            }
        }

        override fun clear() {
            throw UnsupportedOperationException("cannot modify readonly map")
        }

        override fun put(key: String, value: V): V? {
            throw UnsupportedOperationException("cannot modify readonly map")
        }

        override fun putAll(from: Map<out String, V>) {
            throw UnsupportedOperationException("cannot modify readonly map")
        }


        override val entries: MutableSet<MutableMap.MutableEntry<String, V>>
            get() {
                val res = super.entries.map { e -> REntry(e.key, e.value) }.toMutableSet()
                return ReadonlySet(res) as MutableSet<MutableMap.MutableEntry<String, V>>
            }

        private class REntry<V>(override val key: String, override val value: V) :
                MutableMap.MutableEntry<String, V> {
            override fun setValue(newValue: V): V {
                throw UnsupportedOperationException("cannot modify readonly map")
            }
        }

        private  class ReadonlySet<V>(set:MutableSet<V>):MutableSet<V> by set
        {
            override fun add(element: V): Boolean {
                throw UnsupportedOperationException("cannot modify readonly map")
            }

            override fun addAll(elements: Collection<V>): Boolean {
                throw UnsupportedOperationException("cannot modify readonly map")
            }

            override fun clear() {
                throw UnsupportedOperationException("cannot modify readonly map")
            }

            override fun remove(element: V): Boolean {
                throw UnsupportedOperationException("cannot modify readonly map")
            }

            override fun removeAll(elements: Collection<V>): Boolean {
                throw UnsupportedOperationException("cannot modify readonly map")
            }
        }

    }
}




class OrderedCaseInsensitiveMap<V> : CaseInsensitiveKeyMap<V>() {
    //порядок ключей
    private val order = mutableSetOf<String>()

    companion object {
        fun <VV> create(toCopy: Map<String, VV>) = OrderedCaseInsensitiveMap<VV>().also {
            toCopy.forEach { entry -> it[entry.key] = entry.value }
        }
    }

    override fun clear() {
        order.clear()
        return super.clear()
    }

    override fun remove(key: String): V? {
        order.remove(key)
        return super.remove(key)
    }

    override fun put(key: String, value: V): V? {
        if (!order.contains(key))
            order.add(key)
        return super.put(key, value)
    }

    override val keys: MutableSet<String>
        get() = order

    override val values: MutableCollection<V>
        get() = order.map { super.get(it)!! }.toList().toMutableList()


    override val entries: MutableSet<MutableMap.MutableEntry<String, V>>
        get() = order.map { MEntry(it, super.get(it)!!) }.toList().toMutableSet()


    override fun computeIfAbsent(key: String,
                                 mappingFunction: (String) -> V): V {
        val v = get(key)
        if (v == null) {
            val newValue: V? = mappingFunction(key)
            if (newValue != null) {
                put(key, newValue)
                return newValue
            }
        }
        return v!!
    }


}

typealias CiMap<V> = CaseInsensitiveKeyMap<V>


fun <V> ciMapOf(): CaseInsensitiveKeyMap<V> = CaseInsensitiveKeyMap()

fun <V> caseInsMapOf(): CaseInsensitiveKeyMap<V?> = CaseInsensitiveKeyMap()
