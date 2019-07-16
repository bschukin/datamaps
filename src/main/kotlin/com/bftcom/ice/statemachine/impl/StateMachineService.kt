package com.bftcom.ice.statemachine.impl

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.statemachine.State.getDefaultTransition
import com.bftcom.ice.statemachine.StateMachine.findStartState
import com.bftcom.ice.statemachine.StateMachine.findState
import com.bftcom.ice.datamaps.misc.throwImpossible
import com.bftcom.ice.datamaps.misc.throwNotFound
import com.bftcom.ice.datamaps.misc.Timestamp
import com.bftcom.ice.datamaps.impl.mappings.DataMappingsService
import com.bftcom.ice.datamaps.impl.util.CacheClearable
import com.bftcom.ice.datamaps.tools.ServerScriptService
import com.bftcom.ice.datamaps.tools.Var
import com.bftcom.ice.statemachine.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
open class StateMachineServiceImpl(val dataService: DataService,
                                   val dataMappingService: DataMappingsService,
                                   val serverScriptService: ServerScriptService
) : StateMachineService, DataMapTriggers, CacheClearable {

    /** класс описывающий связь Entity и StateMachine:
     *  какие поля могут быть "вынесены" в Entity  от статусной модели:
     *      - stateMachineInstanceId,
     *      - stateId,
     *      - никаких (на данный момент это пока ошибка).
     *
     *  Если поле stateMachineInstanceId отсутствует в entity,
     *          то stateMachineInstance не создается ("легкая стат модель" - не имеет истории перемещений и контекста с переменными).
     *  Если stateMachineInstanceId присутствует, то инстанс stateMachineInstance создается.
     *
     *
     *  Примеры вынесения полей статусной модели в сущности:
     *
     *  1) stateMachineInstanceId    (полноценный, но тяжелый для запросов вариант)
     *  2) stateMachineInstanceId + stateId    (полноценный и более быстрый для запросов вариант)
     *  3) stateId   ("легкая статусная модель")
     */
    private data class EntityStateMachine(val entity: String, val statemachine: String)

    private data class EntityStateMachineInfo(
            val key: EntityStateMachine,
            val hasStateMachineInstanceIdField: Boolean,
            val hasStateIdField: Boolean) {

        fun isNormalStateModel() = hasStateMachineInstanceIdField
        fun isLightStateModel() = !hasStateMachineInstanceIdField && hasStateIdField

    }


    override val targetEntities: List<String>
        get() = listOf(StateMachine.entity, State.entity, Transition.entity)

    private val defaultStateMachines = mutableMapOf<String, String>()
    private val stateMachineCache = mutableMapOf<String, DataMapF<StateMachine>>()

    //карта: state id <-> код статусной модели
    private val stateCache = mutableMapOf<Long, String>()

    private val entityStateMachineInfoCache = mutableMapOf<EntityStateMachine, EntityStateMachineInfo>()

    override fun registerDefaultStateMachine(entity: String, stateMachineCode: String) {
        defaultStateMachines[entity] = stateMachineCode
    }

    override fun getDefaultStateMachine(entity: String): String? {
        return defaultStateMachines[entity]
    }

    override fun getStateMachine(stateMachineCode: String): DataMapF<StateMachine>? {
        return cache()[stateMachineCode]
    }

    private fun getEntityStateMachineInfo(entity: String, stateMachineCode: String): EntityStateMachineInfo {

        val key = EntityStateMachine(entity, stateMachineCode)

        return entityStateMachineInfoCache.getOrPut(key) {
            val dm = dataMappingService.getDataMapping(entity)
            EntityStateMachineInfo(key,
                    dm.findByName(stateMachineInstanceField.n) != null,
                    dm.findByName(stateField.n) != null)
        }
    }

    override fun initStateMachineInstance(target: DataMap, stateMachineCode: String) {
        val theMachine = cache()[stateMachineCode]
                ?: throwNotFound("state machine [$stateMachineCode] was not found")
        val esmInfo = getEntityStateMachineInfo(target.entity, stateMachineCode)

        val theState = theMachine.findStartState()

        if (esmInfo.hasStateIdField)
            target[stateField] = theState

        if (esmInfo.isNormalStateModel()) {
            val sim = StateMachineInstance.createInSilence {
                it[stateMachine] = theMachine
                it[state] = theState
                it[created] = Timestamp()
            }
            dataService.insert(sim)

            if (esmInfo.hasStateMachineInstanceIdField)
                target[stateMachineInstanceField] = sim
        }
    }


    override fun initStateMachineInstance(target: DataMap, stateMachine: DataMapF<StateMachine>) =
            initStateMachineInstance(target, stateMachine[StateMachine.code])

    override fun doTransition(dataMap: DataMap, transition: DataMapF<Transition>?, flushAfterTransition: Boolean) {

        //находим статусную модель и статус
        val context = obtainContext(dataMap, transition)

        execTransition(context, flushAfterTransition)
    }


    override fun doTransitionByCode(dataMap: DataMap, transitionCode: String, flushAfterTransition: Boolean) {

        val context = obtainContext(dataMap, null, transitionCode)

        execTransition(context, flushAfterTransition)
    }


    private class StateTransitionContext(val dataMap: DataMap,
                                         val oldState: DataMapF<State>,
                                         val transition: DataMapF<Transition>,
                                         val smInstance: DataMapF<StateMachineInstance>?,
                                         val esmInfo: EntityStateMachineInfo)


    private fun obtainContext(dataMap: DataMap, transition: DataMapF<Transition>?, transitionCode: String? = null): StateTransitionContext {

        val stateMachineCode = when {
            transition?.get(Transition.state)?.id != null -> getStateMachineCodeByStateId(transition[{ state().id }])
            dataMap[{ stateMachineInstanceField().stateMachine().code }] != null -> dataMap[{ stateMachineInstanceField().stateMachine().code }]
            dataMap[stateField] != null -> getStateMachineCodeByStateId(dataMap[stateField().id])
            else -> throwImpossible()
        }

        val esmInfo = getEntityStateMachineInfo(dataMap.entity, stateMachineCode)

        //находим статусную модель и статус
        val smInstance = when {
            esmInfo.isNormalStateModel() && dataMap[stateMachineInstanceField]!=null -> dataMap[stateMachineInstanceField]
            esmInfo.isNormalStateModel() && dataMap[stateMachineInstanceField]==null->
            {
                dataService.upgrade(listOf(dataMap), on(dataMap.entity).field(stateMachineInstanceField).field("id"))
                dataMap[stateMachineInstanceField]
            }
            else -> null
        }

        val stateMachine = getStateMachine(stateMachineCode)!!

        val currentState =
                when {
                    esmInfo.hasStateIdField -> dataMap[stateField]
                    smInstance != null -> stateMachine.findState(smInstance[{ state().code }])!!
                    else -> throwImpossible()
                }!!

        val aTransition = when {
            transition != null -> stateMachine.findTransitionById(transition.id)
            transitionCode != null -> stateMachine.findTransitionByCode(transitionCode)
            else -> stateMachine.findDefaultTransitionForState(currentState)
        }!!

        return StateTransitionContext(dataMap, oldState = currentState,
                transition = aTransition, smInstance = smInstance, esmInfo = esmInfo)

    }

    private fun execTransition(context: StateTransitionContext,
                               flushAfterTransition: Boolean) {

        //если переход не нашли - ошибка

        //выполняем действия на переходе
        execAction(context.dataMap, context.transition)

        //присваиваем новый статус
        val newState = context.transition[{ target }]

        val newContext = StateTransitionContext(context.dataMap, newState, context.transition, context.smInstance, context.esmInfo)

        if (context.esmInfo.hasStateMachineInstanceIdField)
            context.smInstance!![{ state }] = newState

        if (context.esmInfo.hasStateIdField)
            context.dataMap[stateField] = newState

        if (flushAfterTransition)
            dataService.flush()

        if (newState[{ isExclusiveGateway }] == true) {
            execExclusiveGateway(newContext, flushAfterTransition)
        }
    }

    private fun execExclusiveGateway(context: StateTransitionContext, flushAfterTransition: Boolean) {
        val trueConditionalTransitions = context.oldState[{ transitions }].filter { execConditionScript(context.dataMap, it) }
        if (trueConditionalTransitions.count() == 0) {
            throwNotFound("The transition from state ${context.oldState[{ code }]}")
        }
        if (trueConditionalTransitions.count() > 1) {
            throwImpossible("Too many true conditional transitions from state ${context.oldState[{ code }]}")
        }

        val newContext = StateTransitionContext(context.dataMap, context.oldState,
                trueConditionalTransitions[0], context.smInstance, context.esmInfo)

        execTransition(newContext, flushAfterTransition)
    }


    private fun execAction(theDataMap: DataMap, theTransition: DataMapF<Transition>) {
        val action = theTransition[{ action }]?.to(Action) ?: return
        val script = action[{ script }]
        val data = theDataMap["data"]
        if (script != null) {
            serverScriptService.execute(script,
                    Var("_entity", theDataMap),
                    Var("_data", data),
                    Var("_transition", theTransition),
                    Var("_dataService", dataService, DataService::class.simpleName!!))
        } else
            dataService.springBeanMethodCall(action[{ className }]!!, action[{ method }]!!,
                    args = *arrayOf(theDataMap, theTransition))
    }

    private fun execConditionScript(theDataMap: DataMap, theTransition: DataMapF<Transition>): Boolean {
        val script = theTransition[{ conditionScript }]
        if (script != null && script.isNotEmpty()) {
            val scriptResult = serverScriptService.execute(script, Var("_entity", theDataMap))
            return if (scriptResult is Boolean) scriptResult else false
        }
        return false
    }

    override fun beforeInsert(event: TriggerContext) {
        clearCache()
    }

    override fun afterUpdate(event: TriggerContext) {
        clearCache()
    }

    override fun clearCache() {
        stateMachineCache.clear()
        entityStateMachineInfoCache.clear()
        stateCache.clear()
    }

    private fun cache(): Map<String, DataMapF<StateMachine>> {
        if (stateMachineCache.isEmpty()) {
            stateMachineCache.putAll(dataService.findAll(StateMachine.slice {
                full()
                states {
                    full()
                    transitions {
                        scalars().withRefs().withBlobs()
                    }
                }
            }
            ).map { it[{ code }] to it }.toMap())

            stateCache.clear()
            stateMachineCache.forEach { smCode, sm ->
                sm[{ states }].forEach {
                    stateCache[(it.id as Number).toLong()] = smCode
                }
            }
        }
        return stateMachineCache
    }

    private fun DataMapF<StateMachine>.findTransitionByCode(code: String): DataMapF<Transition>? {
        return this[StateMachine.states].flatMap { it[{ transitions }] }.find { st -> code == st[Transition.code] }
    }

    private fun DataMapF<StateMachine>.findTransitionById(id: Any?): DataMapF<Transition>? {
        return this[StateMachine.states].flatMap { it[{ transitions }] }.find { st -> id == st.id }
    }

    private fun DataMapF<StateMachine>.findDefaultTransitionForState(state: DataMapF<State>): DataMapF<Transition> {
        return (this[StateMachine.states].findById(state.id) as DataMapF<State>).getDefaultTransition()
                ?: throwNotFound("The transition from state ${state[{ code }]}")

    }

    private fun getStateMachineCodeByStateId(stateId: Long): String {
        cache()
        return stateCache[stateId]!!
    }
}