package com.datamaps.services

import com.datamaps.mappings.DataProjection
import com.datamaps.mappings.projection
import com.datamaps.maps.DataMap
import com.datamaps.maps.mergeDataMaps
import org.springframework.stereotype.Service
import javax.annotation.Resource

/**
 * Created by Щукин on 27.10.2017.
 */
interface DataService {

    operator fun get(entityName: String, id: Long): DataMap?

    fun findAll(dp: DataProjection):List<DataMap>

    fun upgrade(maps: List<DataMap>, slice: projection):List<DataMap>
}




@Service
class DataServiceImpl : DataService
{

    @Resource
    lateinit var queryBuilder: QueryBuilder

    @Resource
    lateinit var queryExecutor: QueryExecutor

    override fun get(entityName: String, id: Long): DataMap? {

        val q = queryBuilder.createQueryByEntityNameAndId(entityName, id)
        return queryExecutor.executeSingle(q)
    }

    override fun findAll(dp: DataProjection):List<DataMap> {
        val q = queryBuilder.createQueryByDataProjection(dp)
        return queryExecutor.findAll(q)
    }

    override fun upgrade(maps: List<DataMap>, slice: projection): List<DataMap> {

        if(maps.isEmpty())
            return maps

        //составляем запрос на slice
        val q = queryBuilder.createUpgradeQueryByMapsAndSlices(maps, slice)
        //исполняем запрос
        val sliceMaps  = queryExecutor.findAll(q)

        return mergeDataMaps(maps, sliceMaps)
    }
}