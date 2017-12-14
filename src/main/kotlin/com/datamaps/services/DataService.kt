package com.datamaps.services

import com.datamaps.mappings.DataMapping
import com.datamaps.mappings.DataMappingsService
import com.datamaps.maps.DataProjection
import com.datamaps.maps.projection
import com.datamaps.maps.DataMap
import com.datamaps.maps.mergeDataMaps
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import javax.annotation.Resource

/**
 * Created by Щукин on 27.10.2017.
 */
interface DataService {

    fun get(entityName: String, id: Long): DataMap?

    fun find(dp: DataProjection): DataMap?

    fun findAll(dp: DataProjection):List<DataMap>

    fun upgrade(maps: List<DataMap>, slice: projection):List<DataMap>

    fun delete (datamap: DataMap)

    fun flush()

    fun getDataMapping(name: String): DataMapping
}




@Service
class DataServiceImpl : DataService
{



    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    lateinit var deltaMachine: DeltaMachine

    @Resource
    lateinit var queryBuilder: QueryBuilder

    @Resource
    lateinit var queryExecutor: QueryExecutor

    @Resource
    lateinit var dataMappingsService: DataMappingsService



    override fun get(entityName: String, id: Long): DataMap? {

        val q = queryBuilder.createQueryByEntityNameAndId(entityName, id)
        return queryExecutor.executeSingle(q)
    }

    override fun find(dp: DataProjection): DataMap? {
        val q = queryBuilder.createQueryByDataProjection(dp)

        LOGGER.info("\r\nsql: ${q.sql} \n\t with params ${q.params}")

        val res = queryExecutor.findAll(q)
        if(res.size>1)
            throw RuntimeException("more than one element found")

        return res.firstOrNull()
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

    override fun delete(datamap: DataMap) {
            DeltaStore.delete(datamap)
    }

    override fun flush()
    {
        deltaMachine.flush()
    }

    override fun getDataMapping(name: String): DataMapping {
       return dataMappingsService.getDataMapping(name)
    }
}