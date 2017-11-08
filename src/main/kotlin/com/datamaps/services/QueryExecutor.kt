package com.datamaps.services

import com.datamaps.general.NIY
import com.datamaps.maps.DataMap
import org.springframework.stereotype.Service

/**
 * Created by Щукин on 03.11.2017.
 */
@Service
class QueryExecutor {

    public fun executeSingle(q:SqlQuery): DataMap
    {
        throw NIY();
    }

}