package com.bftcom.ice.common.maps

import com.bftcom.ice.common.general.makeSure
import com.bftcom.ice.common.utils.GUID
import com.bftcom.ice.server.datamaps.DeltaMachine
import com.bftcom.ice.server.util.JsonWriteOptions
import com.bftcom.ice.server.util.toJson
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.*

object DeltaStore {

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
