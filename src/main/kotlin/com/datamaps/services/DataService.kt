package com.datamaps.services

import com.datamaps.mappings.DataMapping
import com.datamaps.mappings.DataMappingsService
import com.datamaps.maps.*
import com.google.gson.GsonBuilder
import kotlinx.coroutines.experimental.async
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

    fun find_(dp: DataProjection): DataMap

    fun findAll(dp: DataProjection): List<DataMap>

    fun upgrade(maps: List<DataMap>, slice: projection): List<DataMap>

    fun delete(datamap: DataMap)

    fun sqlToFlatMaps(entity:String, sql:String, params:Map<String, Any> = mapOf(), idColumn:String = "ID"): List<DataMap>

    fun sqlToFlatMap(entity:String, sql:String, params:Map<String, Any> = mapOf(), idColumn:String = "ID"): DataMap?

    fun flush()

    fun getDataMapping(name: String): DataMapping

    fun toJson(dm: DataMap): String

    fun async():DataServiceAsync
}


interface DataServiceAsync {
    fun find_(dp: DataProjection): AsyncResult<DataMap>
    fun findAll(dp: DataProjection): AsyncResult<List<DataMap>>
}

interface AsyncResult<T> {
    fun  doWithResult(aaa: (m: T) -> Unit)
}

@Service
class DataServiceImpl : DataService
{


    override fun toJson(dm: DataMap): String {

        val gson = GsonBuilder()
                .registerTypeAdapter(DataMap::class.java, DMSerializer2(this))
                .setPrettyPrinting().create()

        return gson.toJson(dm)
    }

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
        if (res.size > 1)
            throw RuntimeException("more than one element found")

        return res.firstOrNull()
    }

    override fun find_(dp: DataProjection): DataMap {
        return find(dp)!!
    }

    override fun findAll(dp: DataProjection): List<DataMap> {
        val q = queryBuilder.createQueryByDataProjection(dp)

        LOGGER.info("\r\nsql: ${q.sql} \n\t with params ${q.params}")

        return queryExecutor.findAll(q)
    }


    override fun sqlToFlatMaps(entity:String, sql:String, params:Map<String, Any>,idColumn:String): List<DataMap>
    {
        LOGGER.info("\r\nnative sql: $sql \n\t with params $params")

        return queryExecutor.sqlToFlatMaps(entity, sql, idColumn, params)
    }

    override fun sqlToFlatMap(entity:String, sql:String, params:Map<String, Any>,idColumn:String): DataMap?
    {
        LOGGER.info("\r\nnative sql: $sql \n\t with params $params")

        return queryExecutor.sqlToFlatMap(entity, sql, idColumn, params)
    }

    override fun upgrade(maps: List<DataMap>, slice: projection): List<DataMap> {

        if (maps.isEmpty())
            return maps

        //составляем запрос на slice
        val q = queryBuilder.createUpgradeQueryByMapsAndSlices(maps, slice)
        //исполняем запрос
        val sliceMaps = queryExecutor.findAll(q)

        return mergeDataMaps(maps, sliceMaps)
    }

    override fun delete(datamap: DataMap) {
        DeltaStore.delete(datamap)
    }

    override fun flush() {
        deltaMachine.flush()
    }

    override fun getDataMapping(name: String): DataMapping {
        return dataMappingsService.getDataMapping(name)
    }

    private val dataServiceAsyncImpl = DataServiceAsyncImpl(this)
    override fun async(): DataServiceAsync {
        return dataServiceAsyncImpl
    }
}

private class DataServiceAsyncImpl(val dataServiceImpl: DataServiceImpl):DataServiceAsync
{

    override fun findAll(dp: DataProjection):AsyncResult<List<DataMap>>{
        val lamda = {
            dataServiceImpl.findAll(dp)
        }

        return AResult(lamda)
    }

    override fun find_(dp: DataProjection): AsyncResult<DataMap>
    {
        val lamda = {
            dataServiceImpl.find_(dp)
        }

        return AResult(lamda)
    }

    private class AResult<T>( var lamda: () -> T):AsyncResult<T>
    {

        override fun doWithResult(resultLamda: (m: T) -> Unit) {
            val deferred = async { lamda() }
            deferred.invokeOnCompletion {
                resultLamda(deferred.getCompleted())
            }
        }
    }
}