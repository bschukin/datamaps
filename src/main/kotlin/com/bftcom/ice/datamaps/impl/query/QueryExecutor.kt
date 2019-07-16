package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.utils.makeSure
import com.bftcom.ice.datamaps.common.maps.*
import com.bftcom.ice.datamaps.DataMapF.Companion.entityDiscriminator
import com.bftcom.ice.datamaps.impl.util.ConvertHelper
import com.bftcom.ice.datamaps.utils.DbException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.convert.ConversionService
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.stereotype.Service
import java.sql.ResultSet

/**
 * Created by Щукин on 03.11.2017.
 */
@Service
class QueryExecutor {

    @Autowired
    private lateinit var namedParameterJdbcTemplate: NamedParameterJdbcOperations

    @Autowired
    private lateinit var dataService: DataService

    @Autowired
    private lateinit var conversionService: ConversionService

    @Autowired
    private lateinit var convertHelper: ConvertHelper




    fun <T : FieldSet> findAll(q: SqlQueryContext): List<DataMapF<T>> {
        val mc = MappingContext(q)

        try {
            namedParameterJdbcTemplate.query(q.sql, toSqlParams(q.params), { resultSet, _ ->
                run {
                    mapRow(resultSet, q, mc)
                }
            })
        } catch (e: DataAccessException) {
            throw DbException(e)
        }

        var res = mc.result()
        q.qr.postMappers.forEach {
            res = it(res, dataService) as List<DataMapF<T>>
        }


        return res as List<DataMapF<T>>
    }

    fun findAllUnion(q: SqlUnionQueryContext): List<DataMap> {
        val mc = UnionMappingContext(q.firstCtx)
        try {
            namedParameterJdbcTemplate.query(q.sql, toSqlParams(q.params), { resultSet, _ ->
                run {
                    mapRow(resultSet, q.firstCtx, mc)
                }
            })
        } catch (e: DataAccessException) {
            throw DbException(e)
        }

        var res = mc.result().map {
            val entity = (it[entityDiscriminator] as String).trim()
            remapKeysToCorrectFields(entity, q.firstCtx, q.qr[entity]!!, it)
        }

        q.firstCtx.qr.postMappers.forEach {
            res = it(res, dataService)
        }

        return res
    }

    private fun remapKeysToCorrectFields(entity: String, firstCtx: SqlQueryContext, ctx: SqlQueryContext, dataMap: DataMap): DataMap {
        if (dataMap.entity.equals(firstCtx.qr.root.dm.name, true))
            return dataMap
        val newDm = DataMap(entity, dataMap.id)
        val oldkeys = dataMap.map.keys.toList()
        val newkeys = ctx.qr.selectedEntityFields.toList()
        assert(newkeys.size == oldkeys.size)

        for (j in 0..oldkeys.size - 2) {
            val newkey = newkeys[j]
            newDm[newkey, true] = dataMap[oldkeys[j]]
        }
        newDm[entityDiscriminator, true] = entity
        return newDm
    }

    fun executeSingle(q: SqlQueryContext): DataMap? {
        val mc = MappingContext(q)
        try {
            namedParameterJdbcTemplate.query(q.sql, toSqlParams(q.params), { resultSet, _ ->
                run {
                    mapRow(resultSet, q, mc)
                }
            })
        } catch (e: DataAccessException) {
            throw DbException(e)
        }

        var res = mc.result().firstOrNull()

        return res
    }

    fun queryForInt(q: SqlQueryContext): Int {
        return namedParameterJdbcTemplate.queryForObject(q.sql, toSqlParams(q.params), { resultSet, _ -> resultSet.getInt(1) })
    }

    private fun mapRow(resultSet: ResultSet, q: SqlQueryContext, mc: MappingContext) {

        if(q.qr.isGroupByQuery()) {
            mc.create(q.qr.rootAlias, q.qr.root.dm.name,  DeltaStore.newGuid(), resultSet)
        }

        q.qr.columnAliases.values.forEach { col ->
            q.qr.columnMappers[col]!!.forEach { mapper -> mapper.invoke(mc, resultSet) }
        }

        mc.clear()
    }


    fun sqlToFlatMaps(entity: String, sql: String, idField: String, params: Map<String, Any>): List<DataMap> {
        val list = mutableListOf<DataMap>()
        namedParameterJdbcTemplate.query(sql, toSqlParams(params), { resultSet, _ ->
            run {
                list.add(mapRowAsFlatMap(resultSet, entity, idField))
            }
        })
        return list
    }

    fun sqlToFlatMap(entity: String, sql: String, idField: String, params: Map<String, Any>): DataMap? {
        val list = mutableListOf<DataMap>()
        namedParameterJdbcTemplate.query(sql, toSqlParams(params), { resultSet, _ ->
            run {
                list.add(mapRowAsFlatMap(resultSet, entity, idField))
            }
        })
        if (list.size > 1)
            throw RuntimeException("more than one row returned")

        return list.firstOrNull()
    }


    private fun mapRowAsFlatMap(resultSet: ResultSet, entity: String, idField: String): DataMap {
        val dm = DataMap(entity, resultSet.getObject(idField))
        var i = 0
        while (i < resultSet.metaData.columnCount) {
            i++
            val columnName = resultSet.metaData.getColumnLabel(i)
            val columnValue = resultSet.getObject(i)
            if (columnName.equals(idField, true))
                continue
            dm[columnName, true] = columnValue
        }
        return dm
    }

    private fun toSqlParams(params: Map<String, Any?>): Map<String, Any?> {
        return params.mapValues { (param, value) ->
            when (value) {
                is Iterable<*> -> value.map { extractSqlObject(it) }
                is Array<*> -> value.map { extractSqlObject(it) }
                else -> extractSqlObject(value)
            }
        }
    }

    private fun extractSqlObject(value: Any?): Any? {
        val classToConvert = convertHelper.getJavaTypeToConvertToDb(value)
        return if (classToConvert != null) {
            conversionService.convert(value, classToConvert)
        } else value
    }

    fun <T : FieldSet> findChilds(list: List<DataMapF<T>>, q: SqlQueryContext,
                                  parentProperty: String,
                                  childsProperty: String,
                                  mergeIntoSourceList: Boolean,
                                  buildHierarchy: Boolean): List<DataMapF<T>> {
        var childs = findAll<T>(q)

        if (buildHierarchy)
            childs = doBuildHierarchy(childs, parentProperty, childsProperty)

        if (mergeIntoSourceList)
            attachChildsToParents(childs, list, parentProperty, childsProperty)

        return childs
    }

    fun <T : FieldSet> findParents(list: List<DataMapF<T>>, q: SqlQueryContext,
                                   parentProperty: String,
                                   mergeIntoSourceList: Boolean,
                                   buildHierarchy: Boolean,
                                   includeLevel0: Boolean): List<DataMapF<T>> {
        var childs = findAll<T>(q)

        if (buildHierarchy)
            childs = doBuildParentsHierarchy(childs, parentProperty)

        if (mergeIntoSourceList) {
            makeSure(includeLevel0)
            list.forEach {target->
                val self = childs.findById(target.id)
                self?.let {
                    target[parentProperty, true] = self[parentProperty]
                }
            }
        }

        return childs
    }

    private fun <T : FieldSet> doBuildParentsHierarchy(parents: List<DataMapF<T>>,
                                                       parentProperty: String): List<DataMapF<T>> {
        if (parents.isEmpty())
            return emptyList()


        val firstLevel = parents.map { it[__level] as Int }.max()!!
        val lastLevel = parents.map { it[__level] as Int }.min()!!
        val res = parents.filter { it[__level] == firstLevel }

        var currChilds = res

        var j = firstLevel
        while (j >= lastLevel) {
            --j
            val currParents = parents.filter { it[__level] == j }
            currChilds.forEach { ch ->
                val par = currParents.find { it.id == ch(parentProperty)?.id }
                par?.let {
                    ch[parentProperty, true] = par
                }
            }
            currChilds = currParents
        }
        return res
    }

    private fun <T : FieldSet> doBuildHierarchy(childs: List<DataMapF<T>>, parentProperty: String, childProperty: String): List<DataMapF<T>> {
        val res = childs.filter { it[__level] == 1 }

        var currParents = res
        val level = childs.last()[__level] as Int
        for (j in 2..level) {
            val curChilds = childs.filter { it[__level] == j }
            attachChildsToParents(curChilds, currParents, parentProperty, childProperty)
            currParents = curChilds
        }
        return res
    }

    private fun <T : FieldSet> attachChildsToParents(curChilds: List<DataMapF<T>>, currParents: List<DataMapF<T>>, parentProperty: String, childProperty: String) {
        curChilds.forEach { ch ->
            val parent = currParents.find { par -> par.id == ch(parentProperty)?.id }
            parent?.let {
                inSilence {
                    parent.list(childProperty).add(ch)
                    ch[parentProperty] = parent
                }
            }
        }
    }
}