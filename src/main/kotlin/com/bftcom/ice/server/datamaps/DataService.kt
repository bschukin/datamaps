package com.bftcom.ice.server.datamaps

import com.bftcom.ice.common.general.DbRecordNotFound
import com.bftcom.ice.common.general.SomethingNotFound
import com.bftcom.ice.common.general.throwNotFound
import com.bftcom.ice.common.maps.*
import com.bftcom.ice.server.datamaps.mappings.DataMapping
import com.bftcom.ice.server.datamaps.mappings.DataMappingsService
import com.bftcom.ice.server.services.DataServiceExtd
import com.bftcom.ice.server.util.findClassByName
import com.bftcom.ice.server.util.findMethodByArguments
import org.jetbrains.annotations.ApiStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Primary
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.lang.reflect.InvocationTargetException
import java.util.stream.Stream
import javax.annotation.Resource


@ApiStatus.Experimental
interface PostFindQueryProcessor {
    fun postProcessQuery(list: List<DataMap>): List<DataMap>
    fun postProcessQuery(single: DataMap?): DataMap?
}


@Primary
@Transactional
@Service("dataService")
open class DataServiceImpl : DataServiceExtd {
    override fun registerPostFindQueryProcessor(processor: PostFindQueryProcessor) {
        queryExecutor.registerPostFindQueryProcessor(processor)
    }

    private val LOGGER = LoggerFactory.getLogger(this.javaClass)

    @Autowired
    private lateinit var deltaMachine: DeltaMachine

    @Resource
    open lateinit var queryBuilder: QueryBuilder

    @Resource
    private lateinit var queryUnionBuilder: QueryUnionBuilder

    @Resource
    private lateinit var queryRecursiveBuilder: QueryRecursiveBuilder

    @Resource
    private lateinit var queryExecutor: QueryExecutor

    @Resource
    private lateinit var dataMappingsService: DataMappingsService

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired
    private lateinit var conversionService: ConversionService

    @Autowired
    private lateinit var dataMapsUtilService: DataMapsUtilService

    @Autowired
    private lateinit var dataMapsProjectionUtilService:DataMapsProjectionUtilService

    override fun get(entityName: String, id: Any?): DataMap? {

        val q = queryBuilder.createQueryByEntityNameAndId(entityName, id)
        LOGGER.info("\r\nsql-get: ${q.sql} \n\t with params ${q.params}")
        return queryExecutor.executeSingle(q)
    }

    override fun <T : FieldSet> find(dp: DataProjectionF<T>): DataMapF<T>? {
        val q = queryBuilder.createQueryByDataProjection(dp)

        LOGGER.info("\r\nsql: ${q.sql} \n\t with params ${q.params}")

        val res = queryExecutor.findAll<T>(q)
        if (res.size > 1)
            throw RuntimeException("more than one element found")

        return res.firstOrNull()
    }

    override fun <T : FieldSet> find_(dp: DataProjectionF<T>): DataMapF<T> {

        //если в опциях проекции, указан вызов сервиса - исполняем и выходим
        dp.options?.serviceMethodCall?.let {
            return execServiceCall(it, dp)
        }

        return find(dp) ?: throw DbRecordNotFound(dp.entity!!, dp.id)
    }



    override fun <T : FieldSet> findAll(dp: DataProjectionF<T>): List<DataMapF<T>> {

        //если в опциях проекции, указан вызов сервиса - исполняем и выходим
        dp.options?.serviceMethodCall?.let {
            return springBeanMethodCall(it.className, it.method, dp) as List<DataMapF<T>>
        }

        //строим и исполняем запрос
        val q = queryBuilder.createQueryByDataProjection(dp)

        LOGGER.info("\r\nsql: ${q.sql} \n\t with params ${q.params}")

        return queryExecutor.findAll(q)

    }

    override fun findAll(entityName: String): List<DataMap> {
        val dp = Projection(entityName).scalars().withRefs()

        val q = queryBuilder.createQueryByDataProjection(dp)

        LOGGER.info("\r\nsql: ${q.sql} \n\t with params ${q.params}")

        return queryExecutor.findAll<UndefinedFieldSet>(q)
    }

    override fun deleteAll(dp: DataProjection) {

        val q = queryBuilder.createDeleteQueryByDataProjection(dp)

        LOGGER.info("\r\nsql delete: ${q.sql} \n\t with params ${q.params}")

        deltaMachine.deleteByProjection(q)
    }

    override fun unionAll(up: UnionProjection): List<DataMap> {
        val q = queryUnionBuilder.createUnionQueryByDataProjections(up)

        LOGGER.info("\r\nsql: ${q.sql} \n\t with params ${q.params}")

        return queryExecutor.findAllUnion(q)
    }

    override fun <T : FieldSet> loadChilds(list: List<DataMapF<T>>, projection: DataProjection?,
                                           options: TreeQueryOptions): List<DataMapF<T>> {
        if (list.isEmpty())
            return emptyList()

        val (parentField, childField) = getParentAndChildFields(options, list, projection)

        val proj = projection ?: projection(list[0].entity)
        proj.filter { f(parentField) IN (list) }

        val q = queryRecursiveBuilder.createRecursiveFindQuery(proj, parentField, true, options.includeLevel0)

        LOGGER.info("\r\nsql: ${q.sql} \n\t with params ${q.params}")

        return queryExecutor.findChilds(list, q, parentField, childField!!, options.mergeIntoSourceList, options.buildHierarchy)
    }

    override fun <T : FieldSet> loadParents(list: List<DataMapF<T>>, projection: DataProjection?,
                                            options: TreeQueryOptions): List<DataMapF<T>> {
        if (list.isEmpty())
            return emptyList()

        val (parentField, childField) = getParentAndChildFields(options, list, projection)

        val proj = projection ?: projection(list[0].entity).scalars()
        proj.filter { f("id") IN (list) }

        val q = queryRecursiveBuilder.createRecursiveFindQuery(proj, parentField, false, options.includeLevel0)

        LOGGER.info("\r\nsql: ${q.sql} \n\t with params ${q.params}")

        return queryExecutor.findParents(list, q, parentField,
                options.mergeIntoSourceList, options.buildHierarchy, options.includeLevel0)
    }


    override fun count(dp: DataProjection): Int {
        val q = queryBuilder.createCountQuery(dp)

        LOGGER.info("\r\nsql: ${q.sql} \n\t with params ${q.params}")

        return queryExecutor.queryForInt(q)
    }

    override fun sqlToFlatMaps(entity: String, sql: String, params: Map<String, Any>, idColumn: String): List<DataMap> {
        LOGGER.info("\r\nnative sql: $sql \n\t with params $params")

        return queryExecutor.sqlToFlatMaps(entity, sql, idColumn, params)
    }

    override fun sqlToFlatMap(entity: String, sql: String, params: Map<String, Any>, idColumn: String): DataMap? {
        LOGGER.info("\r\nnative sql: $sql \n\t with params $params")

        return queryExecutor.sqlToFlatMap(entity, sql, idColumn, params)
    }

    override fun upgrade(maps: List<DataMap>, slice: DataProjection, returnLoadedParts: Boolean): List<DataMap> {

        if (maps.isEmpty())
            return maps

        //составляем запрос на slice
        val q = queryBuilder.createUpgradeQueryByMapsAndSlices(maps, slice)

        LOGGER.info("\r\nupgrade: ${q.sql} \n\t with params ${q.params}")

        //исполняем запрос
        val sliceMaps = queryExecutor.findAll<UndefinedFieldSet>(q)

        return when (returnLoadedParts) {
            true -> sliceMaps
            else -> mergeDataMaps(maps, sliceMaps)
        }
    }

    override fun delete(datamap: DataMap): Boolean {
        DeltaStore.delete(datamap)
        return true
    }

    override fun deleteAll(entity: String) {
        deltaMachine.deleteAll(entity)
    }

    override fun flush() {
        deltaMachine.flush()
    }

    override fun saveDeltas(buckets: List<DeltaBucket>): Map<String, DataMap> {

        val news = buckets.map { it.dm }
                .filter { it.isNew() }.map { it.newMapGuid!! to it }.toMap()
        deltaMachine.createAndExeUpdateStatements(buckets, true)

        return news
    }

    override fun insert(dataMap: DataMap, preInsertAction: ((DataMap) -> Unit)?, runTriggers: Boolean): DataMap {
        deltaMachine.insert(dataMap, preInsertAction, runTriggers)
        return dataMap
    }


    override fun <T : FieldSet>  copy(source: DataMapF<T>): DataMapF<T> {
        return dataMapsUtilService.copy(source) as DataMapF<T>
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override  fun copy(entity: String, id:Any): DataMap{
        val projection = dataMapsProjectionUtilService.getFullImageProjection(entity)
        val dm =  copy(find_(projection.id(id)))
        return dm
    }


    override fun getDataMapping(name: String): DataMapping {
        return dataMappingsService.getDataMapping(name)
    }

    @Autowired
    lateinit var bulkInserter: BulkInserter

    override fun bulkInsert(list: List<DataMap>, runBeingOperations: Boolean, presInsertAction: ((DataMap) -> Unit)?) {

        bulkInserter.bulkInsert(list, runBeingOperations, presInsertAction)
    }

    override fun bulkInsert(stream: Stream<DataMap>, entity: String, runBeingOperations: Boolean, presInsertAction: ((DataMap) -> Unit)?) {

        bulkInserter.bulkInsert(stream, entity, runBeingOperations, presInsertAction)
    }


    private fun <T : FieldSet> execServiceCall(methodCall: ServiceMethodCall, dp: DataProjectionF<T>): DataMapF<T> {
        val list = (springBeanMethodCall(methodCall.className, methodCall.method, dp) as List<DataMapF<T>>)
        if (list.size > 1)
            throw RuntimeException("more than one element found")
        if (list.isEmpty())
            throw DbRecordNotFound(dp.entity!!, dp.id)
        return list[0]
    }

    override fun springBeanMethodCall(beanClass: String, method: String, vararg args: Any?): Any? {

        val clazz = findClassByName(beanClass)!!
        val bean = context.getBean(clazz)
        val refMethod = findMethodByArguments(bean.javaClass, method, args as Array<Any?>)
                ?: throw SomethingNotFound("cannot find method [$method] in [$beanClass]")

        val methodParameterTypes = refMethod.parameterTypes
        val methodArgs = args.mapIndexed { index, arg ->
            conversionService.convert(arg, methodParameterTypes[index])
        }
        try {
            return refMethod.invoke(bean, *methodArgs.toTypedArray())
        } catch (e: InvocationTargetException) {
            throw e.targetException
        }
    }
}

private fun <T : FieldSet> getParentAndChildFields(options: TreeQueryOptions,
                                                   list: List<DataMapF<T>>,
                                                   projection: DataProjection?): Pair<String, String?> {
    val parentField = when {
        options.parentProperty != null -> options.parentProperty
        list[0].fieldSet != null && list[0].fieldSet!!.containsOption(Tree::class) -> list[0].fieldSet!!.getOption(Tree::class)!!.parentField
        projection?.fieldSet != null && projection.fieldSet!!.containsOption(Tree::class) -> projection.fieldSet!!.getOption(Tree::class)!!.parentField
        else -> throwNotFound("a parent field of the hierarchical query is not defined")
    }!!

    val childField = when {
        options.childProperty != null -> options.childProperty
        list[0].fieldSet != null && list[0].fieldSet!!.containsOption(Tree::class) -> list[0].fieldSet!!.getOption(Tree::class)!!.childsField
        projection?.fieldSet != null && projection.fieldSet!!.containsOption(Tree::class) -> projection.fieldSet!!.getOption(Tree::class)!!.childsField
        else -> throwNotFound("a child field of the hierarchical query is not defined")
    }
    return Pair(parentField, childField)
}



@Service
class DataMapsProjectionUtilService(
        val dataMappingsService: DataMappingsService,
        val dataService: DataService
) {
    fun getFullImageProjection(entity: String,
                               parentProjection: DataProjection = on(entity)): DataProjection {

        parentProjection.scalars()
        parentProjection.withRefs()
        parentProjection.withBlobs()
        parentProjection.withFormulas()

        val dm = dataMappingsService.getDataMapping(entity)
        dm.listsGroup.fields
                .forEach { f ->

                    parentProjection.field(f)

                    val dataField = dm[f]

                    val slice = parentProjection.fields[f]
                    getFullImageProjection(dataField.referenceTo(), slice!!)
                }

        return parentProjection
    }
}

@Service
class DataMapsUtilService(
        val dataMappingsService: DataMappingsService,
        val dataService: DataService
) {


    //todo: перенести в DeltaMachibe
    fun updateBackRef(parent: DataMap, slave: DataMap, parentProperty: String, silent: Boolean = true) {

        val backref = getBackRefField(parent, parentProperty)
        slave[backref, silent] = parent
    }

    //todo: перенести в MappingService
    fun getBackRefField(parent: DataMap, parentProperty: String): String {
        val dm = dataMappingsService.getDataMapping(parent.entity)
        val backref = dataMappingsService.getBackRefField(dm, parentProperty)

        return backref
    }

    fun copy(source: DataMap): DataMap {
        val findCopyTargets = mutableSetOf<DataMap>()
        findCopyTargets(source, findCopyTargets)
        return copy(source, findCopyTargets)
    }

    private fun copy(source: DataMap, toCopy: MutableSet<DataMap>, cache: MutableMap<DataMap, DataMap> = mutableMapOf()): DataMap {


        if (cache.containsKey(source))
            return cache[source]!!

        val res = DataMapF(source.fieldSet, source.entity, id = null, isNew = true)
        cache[source] = res

        source.map.keys
                .filter { !it.equals("id", true) }
                .forEach { fieldName ->
                    val value = source[fieldName]
                    when {
                        value is DataMap -> {
                            when (value["isBackRef"]) {
                                true -> res[fieldName] = copy(source[fieldName] as DataMap, toCopy, cache)
                                else -> res[fieldName] =
                                        if (toCopy.contains(value))
                                            copy(value, toCopy, cache)
                                        else
                                            findReferenceByNativeKey(value)
                            }
                        }
                        value is List<*> -> value.forEach {
                            res.list(fieldName).add(copy(it as DataMap, toCopy, cache))
                        }
                        else -> res[fieldName] = source[fieldName]
                    }
                }

        return res
    }

    private fun findReferenceByNativeKey(value: DataMap): DataMap {
        if (value.fieldSet == null)
            return value
        if (value.fieldSet!!.nativeKey.isEmpty())
            return value
        if (value.fieldSet!!.nativeKey.size > 1)
            TODO()
        val key = value.fieldSet!!.nativeKey[0]
        if (!value.map.containsKey(key.fieldName))
            return value

        val res = dataService.find(on(value.entity).field(key).filter(f(key) eq value(value[key])))
        if (res != null)
            return res
        return value
    }

    private fun findCopyTargets(source: DataMap, result: MutableSet<DataMap>, cache: MutableSet<DataMap> = mutableSetOf()) {

        if (cache.contains(source))
            return

        result.add(source)
        cache.add(source)

        source.map.keys
                .filter { !it.equals("id", true) }
                .forEach { fieldName ->
                    val value = source[fieldName]
                    when {
                        (value is DataMap && value.isDynamic()) ->
                            findCopyTargets(value, result, cache)

                        value is List<*> -> value.forEach {
                            findCopyTargets(it as DataMap, result, cache)
                        }
                    }
                }

    }

}