package com.bftcom.ice.datamaps.impl.delta

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.utils.CaseInsensitiveKeyMap
import com.bftcom.ice.datamaps.utils.makeSure
import com.bftcom.ice.datamaps.common.maps.*
import com.bftcom.ice.datamaps.impl.util.JsonFieldDataMapsBuilder
import com.bftcom.ice.datamaps.impl.util.differsFrom
import com.bftcom.ice.datamaps.impl.util.diffs
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Service
import java.util.*
import java.util.stream.Stream
import javax.annotation.Resource


enum class TriggerType {
    BeforeInsert,
    BeforeUpdate,
    BeforeDelete,
    AfterInsert,
    AfterUpdate,
    AfterDelete
}


@Service
class DataMapTriggersRegistry {

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DataMapTriggersRegistry::class.java)
        private const val allEntities = "allEntities"
        private val emptyServices = emptyList<DataMapTriggers>()
    }

    @Autowired
    private lateinit var appCtx: ApplicationContext

    @Autowired
    private lateinit var triggersMediator: TriggersMediator

    val registry: CaseInsensitiveKeyMap<List<DataMapTriggers>> by lazy {
        init()
    }

    fun runTriggers(ops: IdentityHashMap<DeltaBucket, DeltaMachine.DmlType>,
                    buckets: List<DeltaBucket>, isClientChange: Boolean, before: Boolean):List<DeltaBucket> {

        fun triggerEvent(db: DeltaBucket, type: TriggerType, clientChange: Boolean):Boolean {
            LOGGER.trace("before update event")
            val triggers = getTargetEntititesServices(db.dm)
            return triggersMediator.handleEvent(db, triggers, type, clientChange)
        }

        return buckets
                .filter { b ->
                    val type = ops[b]!!
                    val res = !when {
                        before && type == DeltaMachine.DmlType.UPDATE -> triggerEvent(b, TriggerType.BeforeUpdate, isClientChange)
                        !before && type == DeltaMachine.DmlType.UPDATE -> triggerEvent(b, TriggerType.AfterUpdate, isClientChange)

                        before && type == DeltaMachine.DmlType.INSERT -> triggerEvent(b, TriggerType.BeforeInsert, isClientChange)
                        !before && type == DeltaMachine.DmlType.INSERT -> triggerEvent(b, TriggerType.AfterInsert, isClientChange)

                        before && type == DeltaMachine.DmlType.DELETE -> triggerEvent(b, TriggerType.BeforeDelete, isClientChange)
                        !before && type == DeltaMachine.DmlType.DELETE -> triggerEvent(b, TriggerType.AfterDelete, isClientChange)

                        else -> TODO()
                    }
                    res
                }
    }


    fun runInsertTriggers(list: List<DataMap>) {
        runInsertTriggers(list.stream())
    }

    fun runInsertTriggers(stream: Stream<DataMap>
                          , presInsertAction: ((DataMap) -> Unit)? = null): Stream<DataMap> {

        return stream.peek { dm ->

            makeSure(dm.isNew())
            LOGGER.trace("before insert event")
            getTargetEntititesServices(dm).forEach {
                triggersMediator.applyInsertDataMapEvent({ ctx: TriggerContext -> it.beforeInsert(ctx) }, dm)
            }

            presInsertAction?.invoke(dm)
        }
    }


    private fun getTargetEntititesServices(dataMap: DataMap): Set<DataMapTriggers> {
        return getAllEntititesServices().union(
                getEntititesServices(dataMap.entity))
    }

    private fun getAllEntititesServices(): List<DataMapTriggers> {
        return if (registry[allEntities] == null) emptyServices else registry[allEntities]!!
    }

    private fun getEntititesServices(entity: String): List<DataMapTriggers> {
        return if (registry[entity] == null) emptyServices else registry[entity]!!
    }


    fun init(): CaseInsensitiveKeyMap<List<DataMapTriggers>> {
        val res = CaseInsensitiveKeyMap<MutableList<DataMapTriggers>>()
        val list = appCtx.getBeansOfType(DataMapTriggers::class.java)
        list.values.forEach { ser ->
            val ents = ser.targetEntities
            ents.forEach { e ->
                if (!res.containsKey(e))
                    res[e] = mutableListOf()
                res[e]!!.add(ser)
            }
        }
        res.values.forEach {
            it.sortBy { it.getPriority() }
        }
        return res as CaseInsensitiveKeyMap<List<DataMapTriggers>>
    }

}


@Service
internal class TriggersMediator {


    @Autowired
    private lateinit var dataService: DataService

    @Resource
    private lateinit var jsonFieldDataMapsBuilder: JsonFieldDataMapsBuilder


    fun handleEvent(db: DeltaBucket,
                    triggers: Set<DataMapTriggers>,
                    type: TriggerType,
                    clientChange: Boolean):Boolean {
        //inSilence {
        val ctx = createTriggerContext(db, clientChange)
        val ctx0 = createTriggerContext(db, clientChange)

        triggers.forEach {
            when (type) {
                TriggerType.BeforeInsert -> it.beforeInsert(ctx)
                TriggerType.BeforeUpdate -> it.beforeUpdate(ctx)
                TriggerType.BeforeDelete -> it.beforeDelete(ctx)
                TriggerType.AfterUpdate -> it.afterUpdate(ctx)
                TriggerType.AfterInsert -> it.afterInsert(ctx)
                TriggerType.AfterDelete -> it.afterDelete(ctx)
            }
        }
        if(ctx.cancel)
            return true

        if (!ctx.delta.differsFrom(ctx0.delta, true))
            return false

        val newDeltas = ctx.delta.diffs(ctx0.delta, false)
        newDeltas.forEach {
            db.addDelta(it)
        }

        // ищем все свойства на которые дельты были удалены
        ctx0.delta.map.keys
                .filter { !ctx.delta.map.keys.contains(it) }
                .forEach { db.removeAllPropertyDeltas(it) }

        //}
        return false
    }

    fun applyInsertDataMapEvent(eventHandler: (TriggerContext) -> Unit, dm: DataMap) {
        inSilence {
            val ctx = createTriggerContext(dm)
            eventHandler.invoke(ctx)
        }
    }


    private fun createTriggerContext(db: DeltaBucket, clientChange: Boolean): TriggerContext {
        var cc: TriggerContextImpl? = null
        inSilence {
            val delta = db.dm.toSilentHeaderF()
            if (db.deltas.isNotEmpty() && db.deltas[0].type != DeltaType.DELETE) {
                db.deltas
                        .filter { !DataMap.ID.equals(it.property, true) }
                        .filter { it.type !in listDeltaTypes }
                        .forEach {
                            delta[it.property!!] =
                                    if (it.isJsonOp)
                                        jsonFieldDataMapsBuilder.buildDataMapFromJson(
                                                delta, it.property!!, it.newValue?.value() as String)
                                    else
                                        it.newValue?.value()
                        }
            }

            cc = TriggerContextImpl(delta, clientChange, dataService, _new = if (clientChange) null else db.dm)
        }
        return cc!!
    }

    private fun <T : FieldSet> DataMapF<T>.toSilentHeaderF(): DataMapF<*> {

        val fs = if(this.fieldSet==null)
            FieldSetRepo.fieldSetOrNull(entity) else fieldSet
        val dm = when (fs) {
            null -> SilentDataMapF(UndefinedFieldSet, entity, this.id, isNew())
            else -> SilentDataMapF(fs, entity, this.id, false)
        }

        dm.newMapGuid = newMapGuid

        return dm
    }

    private fun createTriggerContext(dm: DataMap): TriggerContext {
        return TriggerContextImpl(dm, false, dataService)
    }

    private class TriggerContextImpl(override val delta: DataMap,
                                     override val isClientChange: Boolean,
                                     private val dataService: DataService,
                                     private var _old: DataMap? = null,
                                     private var _new: DataMap? = null) : TriggerContext {

        override var cancel:Boolean = false

        override fun old(): DataMap? {
            if (_old == null)
                _old = dataService.find(on(delta.entity).id(delta.id).withRefs().withBlobs())
            return (_old as? DataMapF)?.toReadonly()
        }

        override fun new(): DataMap {
            if (_new == null)
                TODO()//собрать состояние на основе old() + deltas
            return _new!!.toReadonly()
        }
    }
}
