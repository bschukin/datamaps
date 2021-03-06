package com.bftcom.ice.datamaps.core.delta

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.core.util.JsonWriteOptions
import com.bftcom.ice.datamaps.core.util.toJson
import com.bftcom.ice.datamaps.misc.FieldType
import com.bftcom.ice.datamaps.misc.GUID
import com.bftcom.ice.datamaps.misc.makeSure
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.*

internal  object DeltaStore {

    fun newGuid(): String {
        return UUID.randomUUID().toString()
    }
    fun newGUID(): GUID {
        return GUID(UUID.randomUUID().toString())
    }


    internal var context = ThreadLocal<NamedTransactionContext>()
    internal lateinit var deltaMachine: DeltaMachine

    fun delta(dm: DataMap, property: String, oldValue: Any?, newValue: Any?) {
        if (notInTransactionOrSilence()) return

        Delta.change(dm, property, oldValue, newValue)?.let {
            context.get().current().deltas.add(it)
        }
    }

    fun create(dm: DataMap) {
        if (notInTransactionOrSilence()) return

        Delta.create(dm)?.let {
            context.get().current().deltas.add(it)
        }

    }

    fun delete(dm: DataMap) {
        if (notInTransactionOrSilence()) return
        context.get().current().deltas.add(Delta(DeltaType.DELETE, dm))
    }

    fun listAdd(parent: DataMap, child: DataMap, property: String, index:Int) {
        if (notInTransactionOrSilence()) return

        Delta.addToList(parent, child, property, index)?.let {
            context.get().current().deltas.add(it)
            if (it.type == DeltaType.ADD_TO_LIST)
                deltaMachine.updateBackRef(parent, child, property)
        }
    }

    fun listRemove(parent: DataMap, child: DataMap, property: String) {
        if (notInTransactionOrSilence()) return

        Delta.removeFromList(parent, child, property)?.let {
            context.get().current().deltas.add(it)
        }

    }

    private fun notInTransactionOrSilence(): Boolean {
        return notInTransaction() || context.get().silenceValue > 0
    }

    private fun notInTransaction(): Boolean {
        if (!TransactionSynchronizationManager.isActualTransactionActive())
            return true

        if (context.get() == null) {
            context.set(startTransactionContext())
        }
        return false
    }

    private fun startTransactionContext(): NamedTransactionContext {
        return NamedTransactionContext()
    }

    private fun clearTransactionContext() {
        if (context.get() != null) {
            val delete = context.get().clearCurrent()
            if (delete)
                context.remove()
        }
    }

    fun isEmpty(): Boolean {
        return context.get() == null || context.get().current().deltas.isEmpty()
    }

    //отправляет все накопленные изменения в базу
    fun flushAllToBuckets(): List<DeltaBucket> {
        //создаем бакеты: наборы изменений по одному объекту
        //один букет - одна DML ооперация
        val buckets = collectBuckets()

        clearTransactionContext()

        return buckets
    }

    //удаляет все накопленные изменения по карте
    fun removeChanges(dm: DataMap) {
        context.get()?.let {
            it.current().deltas.removeIf { it.dm == dm }
        }
    }

    //получить все измененные и созданные сущностЯ в данной транзакции
    internal fun getChangedEntities(): List<DataMap> {
        if (context.get() == null)
            return emptyList()

        return context.get().current().deltas.map { it.dm }
                .filterNotNull().distinct()
    }

    fun collectBuckets(): List<DeltaBucket> {
        if (context.get() == null)
            return emptyList()

        var lastDM: DataMap? = null
        var currBucket: DeltaBucket? = null
        val res = mutableListOf<DeltaBucket>()

        val deltas = packJsonDeltas(
                smartDeltaSort(context.get().current().deltas)
        ).map { transformDelta(it) }

        deltas.forEach { delta ->
            if (delta.type == DeltaType.CREATE) {
                lastDM = null
                currBucket = DeltaBucket(delta.dm!!)
            }
            if (lastDM != delta.dm) {
                currBucket = DeltaBucket(delta.dm!!)
                lastDM = delta.dm
                res.add(currBucket!!)
            }
            if (delta.type != DeltaType.CREATE)
                currBucket!!.addDelta(delta)
        }

        return res.filter {  db-> !(db.dm.isNew() && db.deltas.last().type == DeltaType.DELETE) }
    }

    /**
     * трансформация букета при необходимости.
     * На данный момент нужна чтобы заменить несколько отдельных json-изменений в единственное изменение по json-колонке
     */
    private fun transformDelta(delta: Delta): Delta {
        if (!delta.isJsonOp)
            return delta

        //формируем новое состояние json как сериализацию мапов
        val propValue = delta.dm!![delta.property!!]
        val json = propValue!!
                .toJson(JsonWriteOptions(false, true))

        return Delta(delta.type, delta.dm!!, delta.property!!, oldValue = null, newValue = AnyValue(json),
                parent = delta.parent, parentProperty = delta.parentProperty,
                isJsonOp = true, jsonSubDeltas = delta.jsonSubDeltas)
    }

    class TransactionSynchronizationAdapter : TransactionSynchronization {

        override fun beforeCompletion() {
            clearTransactionContext()
        }


        override fun beforeCommit(readOnly: Boolean) {
            deltaMachine.flush()
        }
    }

    class NamedTransactionContext(private val map: MutableMap<String, TransactionContext> = mutableMapOf(),
                                  var silenceValue: Int = 0) {
        fun current(): TransactionContext {
            val name = TransactionSynchronizationManager.getCurrentTransactionName() ?: "___"

            var v = map.get(name);
            if (v == null) {
                val tsa = TransactionSynchronizationAdapter()
                TransactionSynchronizationManager.registerSynchronization(tsa)
                v = TransactionContext()
                map.put(name, v)
            }
            return v
        }

        fun clearCurrent(): Boolean {
            val name = TransactionSynchronizationManager.getCurrentTransactionName() ?: "___"
            val ctx = map[name]
            if (ctx != null) {
                ctx.clear()
                map.remove(name)
            }
            return map.isEmpty()
        }

        fun silence() = silenceValue++
        fun noise() {
            silenceValue--
            makeSure(silenceValue >= 0)
        }
    }

    internal fun silence() {
        if (notInTransaction()) return

        context.get().silence()
    }

    internal fun noise() {
        if (notInTransaction()) return

        context.get().noise()
    }


}

internal enum class DeltaType(override val value: String) : StringEnum {
    CREATE("CREATE"),
    DELETE("DELETE"),
    VALUE_CHANGE("VALUE_CHANGE"),
    ADD_TO_LIST("ADD_TO_LIST"),
    DELETE_FROM_LIST("DELETE_FROM_LIST");

}

internal val listDeltaTypes = listOf(DeltaType.DELETE, DeltaType.ADD_TO_LIST, DeltaType.DELETE_FROM_LIST)

internal class JsonSubDelta(val jsonPath: String, var oldValue: AnyValue? = null, var newValue: AnyValue? = null)

//атомарное изменение
//NB: не разношу по разным классам, чтобы не втягиваться в запарки с сериализацией
internal class Delta(val type: DeltaType, val dm: DataMap? = null, var property: String? = null,
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

//набор изменений по одному мапу
//один букет - одна DML ооперация
internal class DeltaBucket(val dm: DataMap,
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





internal fun packJsonDeltas(deltas: List<Delta>): List<Delta> {
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

internal fun smartDeltaSort(deltas: List<Delta>): List<Delta> {

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



internal data class TransactionContext(
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

internal class JsonPath(aid: String) {
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