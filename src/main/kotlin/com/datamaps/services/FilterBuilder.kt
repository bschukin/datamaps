package com.datamaps.services

import com.datamaps.general.validateNIY
import org.springframework.stereotype.Service

/**
 * Created by b.schukin on 21.11.2017.
 */
@Service
class FilterBuilder {


    fun buildWhere(qr: QueryBuildContext) {

        val querylevel = qr.stack.peek()
        val projection = querylevel.dp

        //пока мы строим только запрос по ID
        projection.id?.let {
            validateNIY(projection.isRoot())

            qr.where = "${querylevel.alias}.${querylevel.dm.idColumn} = :_id1"
            qr.params["_id1"] = projection.id
        }
    }

}