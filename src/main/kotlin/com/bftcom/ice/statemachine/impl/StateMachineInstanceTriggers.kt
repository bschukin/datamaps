package com.bftcom.ice.statemachine.impl

import com.bftcom.ice.statemachine.StateMachineService
import com.bftcom.ice.datamaps.DataMapTriggers
import com.bftcom.ice.datamaps.DataService
import com.bftcom.ice.datamaps.TriggerContext
import com.bftcom.ice.statemachine.stateMachineInstanceField
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
}