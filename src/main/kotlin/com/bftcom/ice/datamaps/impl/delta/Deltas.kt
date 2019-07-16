package com.bftcom.ice.datamaps.impl.delta

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.misc.FieldType
import com.bftcom.ice.datamaps.misc.makeSure

//набор изменений по одному мапу
//один букет - одна DML ооперация
class DeltaBucket(val dm: DataMap,
                  val deltas: MutableList<Delta> = mutableListOf(),
                  val forceNew: Boolean = false)
{
    val displayName: String

    init {
        this.displayName = dm.getDisplayName()
    }

    fun isDelete() = !deltas.isEmpty() && deltas.first().type == DeltaType.DELETE

    fun addDelta(delta: Delta) {
        val remove = deltas.filter { it.property.equals(delta.property, true) }.toList()

        val oldValue = remove.firstOrNull()?.oldValue
        remove.forEach {
            deltas.remove(it)
        }

        if (remove.isNotEmpty())
            delta.oldValue = oldValue

        deltas.add(delta)
    }

    fun removeAllPropertyDeltas(property: String) {
        val remove = deltas.filter { it.property.equals(property, true) }.toList()
        remove.forEach {
            deltas.remove(it)
        }
    }

    fun addChange(property: String, value: Any?) {

        deltas.add(Delta(DeltaType.VALUE_CHANGE, null, property, null, AnyValue(value)))
    }

    override fun toString(): String {
        return "DeltaBucket(dm=$dm)"
    }

}





fun packJsonDeltas(deltas: List<Delta>): List<Delta> {
    val res = mutableListOf<Delta>()
    val map = mutableMapOf<Pair<DataMap?, String?>, Delta>()

    deltas.forEach { d ->
        if (!d.isJsonOp)
            res.add(d)
        else {
            val key = Pair(d.dm, d.property)
            if (!map.containsKey(key)) {
                map[key] = d
                res.add(d)
            }
            val firstJsonDelta = map[key]!!
            firstJsonDelta.addJsonSubDelta(JsonSubDelta(d.jsonPath!!, d.oldValue, d.newValue))
            firstJsonDelta.oldValue = null
        }

    }
    return res
}

fun smartDeltaSort(deltas: List<Delta>): List<Delta> {

    val input = deltas.toMutableList()
    val res = mutableListOf<Delta>()

    val creates = mutableListOf<Delta>()

    fun parentIsCreatedAbove(parent: DataMap): Boolean {
        return creates.find { it.dm == parent } != null
    }

    fun isNewDataMap(any: AnyValue?): Boolean {
        return any != null && any._dm != null && any._dm!!.isNew() &&
                !parentIsCreatedAbove(any._dm!!)
    }

    fun isNewDataMap(any: DataMap?): Boolean {
        return any != null && any.isNew() && !parentIsCreatedAbove(any)
    }

    //TODO: адски неээфективный алгоритм для больших массивов. надо придумать другой
    while (input.isNotEmpty()) {
        val curr = input.removeAt(0)
        res.add(curr)
        if (curr.type == DeltaType.CREATE)
            creates.add(curr)

        val dm = curr.dm
        val each = input.iterator()
        while (each.hasNext()) {
            val it = each.next()
            val flag = it.dm == dm && (!isNewDataMap(it.newValue) && !isNewDataMap(it.parent))
            if (flag) {
                res.add(it)
                each.remove()
            }
        }
    }

    return res
}




enum class DeltaType(override val value: String) : StringEnum {
    CREATE("CREATE"),
    DELETE("DELETE"),
    VALUE_CHANGE("VALUE_CHANGE"),
    ADD_TO_LIST("ADD_TO_LIST"),
    DELETE_FROM_LIST("DELETE_FROM_LIST");


}

val listDeltaTypes = listOf(DeltaType.DELETE, DeltaType.ADD_TO_LIST, DeltaType.DELETE_FROM_LIST)

class JsonSubDelta(val jsonPath: String, var oldValue: AnyValue? = null, var newValue: AnyValue? = null)

//атомарное изменение
//NB: не разношу по разным классам, чтобы не втягиваться в запарки с сериализацией
class Delta(val type: DeltaType, val dm: DataMap? = null, var property: String? = null,
            var oldValue: AnyValue? = null, var newValue: AnyValue? = null,
            val parent: DataMap? = null, val parentProperty: String? = null,
            val isJsonOp: Boolean = false,
            val jsonPath: String? = null,
            var jsonSubDeltas: MutableList<JsonSubDelta>? = null) {

    fun addJsonSubDelta(ds: JsonSubDelta) {
        if (jsonSubDeltas == null)
            jsonSubDeltas = mutableListOf()

        val remove = jsonSubDeltas!!.filter { it.jsonPath == ds.jsonPath }.toList()
        val oldValue = remove.firstOrNull()?.oldValue
        remove.forEach {
            jsonSubDeltas!!.remove(it)
        }
        if (remove.size > 0)
            ds.oldValue = oldValue
        jsonSubDeltas!!.add(ds)
    }

    //статические методы для создания  различных дельт на основе входящих изменений
    companion object {

        fun change(dm: DataMap, property: String, oldValue: Any?, newValue: Any?): Delta? {

            if (dm.isTransient())
                return null
            //если изменение пришло для динамической ентити
            //то мы его оформляем на владельца этой динамической сущности
            return if (dm.isDynamic()) {

                val holder = writeJsonSystemFieldsIfShould(newValue, dm, property)

                if (dm[DataMap.JSON_PATH] == null || holder == null)
                    return null

                val jsonPath = JsonPath(dm[DataMapF.JSON_PATH] as String)
                val jp = if (jsonPath.path.isBlank()) property else "${jsonPath.path}.$property"
                Delta(DeltaType.VALUE_CHANGE, holder, jsonPath.property,
                        isJsonOp = true, jsonPath = jp,
                        oldValue = AnyValue(oldValue), newValue = AnyValue(newValue))
            } else {

                writeJsonSystemFieldsIfShould(newValue, dm, property)

                Delta(DeltaType.VALUE_CHANGE, dm, property, AnyValue(oldValue), AnyValue(newValue),
                        isJsonOp = newValueIsJson(newValue), jsonPath = "")
            }
        }


        private fun writeJsonSystemFieldsIfShould(newValue: Any?, dm: DataMap, property: String): DataMap? {
            val holder = (if (dm.isDynamic()) dm[DataMapF.DYNAMIC_HOLDER] else dm) as? DataMap?
            val isDynamicDataMap = newValueIsJson(newValue)

            if (isDynamicDataMap && (newValue as DataMap).isNew()) {
                newValue[DataMapF.DYNAMIC_HOLDER, true] = holder
                val jp = JsonPath.buildJSonPathString(dm[DataMapF.JSON_PATH] as? String, property)
                newValue[DataMapF.JSON_PATH, true] = jp
            }

            return holder
        }

        fun create(dm: DataMap): Delta? {
            if (dm.isTransient())
                return null

            //если создание для динамической ентити - то мы смело можем его игнорить,
            //так как следом пойдет присвоением этой ентити в объект
            //и мы отловим изменение там
            return if (dm.isDynamic())
                return null
            else
                Delta(DeltaType.CREATE, dm)
        }

        fun addToList(parent: DataMap, child: DataMap, property: String, index: Int): Delta? {

            if (parent.isTransient())
                return null

            //могут быть три ситуации:
            //1) добавление  в список принадлежащий динамической сущности
            //мы его оформляем как изменение владельца этой динамической сущности
            return if (parent.isDynamic()) {
                val holder = parent[DataMap.DYNAMIC_HOLDER] as DataMap?
                if (parent[DataMapF.JSON_PATH] == null || holder == null)
                    return null
                val jsonPath = JsonPath(parent[DataMapF.JSON_PATH] as String)
                val jp = (if (jsonPath.path.isBlank()) property else "${jsonPath.path}.$property") + "[$index]"

                Delta(DeltaType.VALUE_CHANGE, holder, jsonPath.property,
                        isJsonOp = true, jsonPath = jp)
            }
            //2) добавление динамической сущности в список принадлежащий реляционной сущности -
            //мы его оформляем как изменение владельца этой динамической сущности
            else if (child.isDynamic()) {
                val jsonPath = JsonPath(child[DataMapF.JSON_PATH] as String)
                Delta(DeltaType.VALUE_CHANGE, parent, property, isJsonOp = true, jsonPath = jsonPath.path)
            } else
            //просто изменение списка
                Delta(DeltaType.ADD_TO_LIST, child, "", null, null,
                        parent, property)
        }

        fun removeFromList(parent: DataMap, child: DataMap, property: String): Delta? {
            //могут быть три ситуации:
            //1) удаление  из списка принадлежащий динамической сущности
            //мы его оформляем как изменение владельца этой динамической сущности
            return if (parent.isDynamic()) {
                val holder = parent[DataMap.DYNAMIC_HOLDER] as DataMap?
                if (parent[DataMapF.JSON_PATH] == null || holder == null)
                    return null
                val jsonPath = JsonPath(parent[DataMapF.JSON_PATH] as String)
                val jp = (if (jsonPath.path.isBlank()) property else "${jsonPath.path}.$property") /*todo + "[$index]"*/
                Delta(DeltaType.VALUE_CHANGE, holder, jsonPath.property, isJsonOp = true, jsonPath = jp)
                //2) удаление из динамической сущности в список принадлежащий реляционной сущности -
                //мы его оформляем как изменение владельца этой динамической сущности
            } else if (child.isDynamic()) {
                Delta(DeltaType.VALUE_CHANGE, parent, property, isJsonOp = true)
            } else
                Delta(DeltaType.DELETE, child)
        }


        private fun newValueIsJson(newValue: Any?): Boolean {
            return newValue is DataMap && newValue.isDynamic()
        }


    }

    override fun toString(): String {
        return "Delta(type=$type, dm=$dm, property=$property, newValue=$newValue)"
    }

}

class JsonPath(aid: String) {
    val property: String
    val path: String

    init {
        val arr = aid.split(':')
        property = arr[2]
        path = if (arr.size > 3) arr[3] else ""
    }

    override fun toString(): String {
        return "::$property:$path"
    }

    companion object {
        fun buildJSonPathString(head: String?, tail: String):String {
            if (head == null)
                return "::$tail"

            val jp = JsonPath(head)
            val jps = jp.toString()
            if(jp.path.isNotBlank())
                return "$jps.$tail"

            return jps +(if(jps.endsWith(":")) "" else ":") + tail
        }
    }
}



data class TransactionContext(
        val deltas: MutableList<Delta> = mutableListOf(),
        var silenceValue: Int = 0
) {
    fun clear() {
        deltas.clear()
    }

    fun silence() = silenceValue++
    fun noise() {
        silenceValue--
        makeSure(silenceValue >= 0)
    }
}

class AnyValue {
    var _simple: TypedValue? = null
    var _dm: DataMap? = null

    constructor()

    constructor(value: Any?) {
        _simple = if (value is DataMap || value == null) null else TypedValue(value)
        _dm = if (value is DataMap) value else null
    }

    fun value(): Any? = if (_simple != null) _simple!!.value() else _dm
    override fun toString(): String {
        return "AnyValue(_simple=$_simple)"
    }


}

/*класс для сериализации простых типов (включая даты) с клиента на сервер.
* Для каждого простого значения кооторое прилетает к нам в конструктор
* мы сохраняем его тип.
* При отдаче на клиента, в сооответсии с типом выбирается способ передачи значения (prepareToTransport)
* При получении значения (неважно где - клиент-сервер), мы проверяем тип и если надо
* преобразуем транспортное значение в значение соотвествуюешее типу
* */
//todo: перенести в пакет com.bftcom.ice.common.misc
class TypedValue {
    constructor()

    constructor(value: Any) {
        _type = FieldType.getByValue(value)
        _value = value
    }

    var _type: FieldType? = null
    var _value: Any? = null

    fun value(): Any? {
        return extractFromTransport()
    }

    //конвертируем значение  для транспорта с клиента на сервер
    fun prepareToTransport() {
        _value = _type?.convertForTransport(_value)
    }

    //проверяем что значение находится в типе соотвествующем  FieldType
    //при необходимости расконвертируем из транспортного значания
    private fun extractFromTransport(): Any? {
        _type?.let {
            if (it.shouldExtractFromTransport(_value))
                _value = it.extractFromTransport(_value)
        }
        return _value
    }

    companion object {

        fun convertMapToValueOrSkipIfCannot(value: Any?): Any? {
            if (value !is Map<*, *>)
                return value
            if (!mapIsTypedValue(value))
                return value

            return mapToValue(value)
        }


        fun mapIsTypedValue(map: Map<*, *>) =
                map.containsKey(TypedValue::_type.name) && map.containsKey(TypedValue::_value.name)

        fun mapToValue(map: Map<*, *>) =
                TypedValue()
                        .apply {
                            this._type = FieldType.valueOf(map[TypedValue::_type.name] as String)
                            this._value = map[TypedValue::_value.name]
                        }.value()
    }
}

//todo: перенести в пакет com.bftcom.ice.common.misc
data class IntHolder(var v: Int = 0) {
    fun incInt(): Int {
        v += 1
        return v
    }
}