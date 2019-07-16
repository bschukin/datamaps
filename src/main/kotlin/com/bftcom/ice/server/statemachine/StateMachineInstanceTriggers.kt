package com.bftcom.ice.server.statemachine

import com.bftcom.ice.common.StateMachineService
import com.bftcom.ice.common.maps.DataMapTriggers
import com.bftcom.ice.common.maps.DataService
import com.bftcom.ice.common.maps.TriggerContext
import com.bftcom.ice.common.stateMachineInstanceField
import org.springframework.stereotype.Service

@Service
class StateMachineInstanceTriggers(val dataService: DataService,
                                   val stateMachineService: StateMachineService) : DataMapTriggers {


    override val targetEntities: List<String>
        get() = DataMapTriggers.allEntities

    override fun beforeInsert(event: TriggerContext) {
        if (event.delta[stateMachineInstanceField.n] != null)
            return

        val s = stateMachineService.getDefaultStateMachine(event.delta.entity) ?: return

        stateMachineService.initStateMachineInstance(event.delta, s)
    }

    /*override fun beforeDelete(event: TriggerContext) {
        if (event.new()[stateMachineInstanceField.n] == null)
            return

        dataService.deleteAll(StateMachineInstance.withId(event.new()[stateMachineInstanceField].id))
    }*/
}