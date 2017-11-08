package com.datamaps.services

import com.datamaps.mappings.DataMapping
import com.datamaps.maps.DataMap
import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by Щукин on 27.10.2017.
 */
interface DataService {

    operator fun get(entityName: String, id: Long): DataMap

}

class SqlQuery(val sql: String, val dataMapping: DataMapping, val params: Map<String, Any>) {
}


@Service
class DataServiceImpl : DataService
{
    @Resource
    lateinit var queryBuilder: QueryBuilder;

    @Resource
    lateinit var queryExecutor: QueryExecutor;

    override fun get(entityName: String, id: Long): DataMap {

        val q = queryBuilder.createQueryByEntityNameAndId(entityName, id);
        return queryExecutor.executeSingle(q);
    }
}