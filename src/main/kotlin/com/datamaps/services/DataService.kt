package com.datamaps.services

import com.datamaps.mappings.DataProjection
import com.datamaps.maps.DataMap
import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by Щукин on 27.10.2017.
 */
interface DataService {

    operator fun get(entityName: String, id: Long): DataMap?

    fun findAll(dp: DataProjection):List<DataMap>
}




@Service
class DataServiceImpl : DataService
{


    @Resource
    lateinit var queryBuilder: QueryBuilder;

    @Resource
    lateinit var queryExecutor: QueryExecutor;

    override fun get(entityName: String, id: Long): DataMap? {

        val q = queryBuilder.createQueryByEntityNameAndId(entityName, id);
        return queryExecutor.executeSingle(q);
    }

    override fun findAll(dp: DataProjection):List<DataMap> {
        val q = queryBuilder.createQueryByDataProjection(dp)
        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        return queryExecutor.findAll(q)
    }
}