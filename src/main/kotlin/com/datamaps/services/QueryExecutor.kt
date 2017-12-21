package com.datamaps.services

import com.datamaps.maps.DataMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet
import javax.annotation.PostConstruct

/**
 * Created by Щукин on 03.11.2017.
 */
@Service
class QueryExecutor {

    @Autowired
    lateinit var namedParameterJdbcTemplate: NamedParameterJdbcOperations

    @Autowired
    lateinit var dataService: DataService

    @Autowired
    lateinit var sqlStatistics: SqlStatistics

    @PostConstruct
    fun init() {
        namedParameterJdbcTemplate = JdbcTemplateWrapper(
                namedParameterJdbcTemplate as NamedParameterJdbcTemplate,
                sqlStatistics)
    }


    fun findAll(q: SqlQueryContext): List<DataMap> {
        val mc = MappingContext(q)
        namedParameterJdbcTemplate.query(q.sql, q.params, { resultSet, _ ->
            run {
                mapRow(resultSet, q, mc)
            }
        })

        var res = mc.result()
        q.qr.postMappers.forEach {
            res = it(res, dataService)
        }
        return res
    }

    fun executeSingle(q: SqlQueryContext): DataMap? {
        val mc = MappingContext(q)
        namedParameterJdbcTemplate.query(q.sql, q.params, { resultSet, _ ->
            run {
                mapRow(resultSet, q, mc)
            }
        })

        return mc.result().firstOrNull()
    }


    fun mapRow(resultSet: ResultSet, q: SqlQueryContext, mc: MappingContext) {
        q.qr.columnAliases.values.forEach { col ->
            q.qr.columnMappers[col]!!.forEach { mapper -> mapper.invoke(mc, resultSet) }
        }
        mc.clear()
    }


    fun sqlToFlatMaps(entity: String, sql: String, idField: String, params: Map<String, Any>): List<DataMap> {
        val list = mutableListOf<DataMap>()
        namedParameterJdbcTemplate.query(sql, params, { resultSet, _ ->
            run {
                list.add(mapRowAsFlatMap(resultSet, entity, idField))
            }
        })
        return list
    }

    fun sqlToFlatMap(entity: String, sql: String, idField: String, params: Map<String, Any>): DataMap? {
        val list = mutableListOf<DataMap>()
        namedParameterJdbcTemplate.query(sql, params, { resultSet, _ ->
            run {
                list.add(mapRowAsFlatMap(resultSet, entity, idField))
            }
        })
        if (list.size > 1)
            throw RuntimeException("more than one row returned")

        return list.firstOrNull()
    }

    fun mapRowAsFlatMap(resultSet: ResultSet, entity: String, idField: String): DataMap {
        val dm = DataMap(entity, resultSet.getObject(idField))
        var i = 0
        while (i < resultSet.metaData.columnCount) {
            i++
            val columnName = resultSet.metaData.getColumnLabel(i)
            val columnValue = resultSet.getObject(i)
            if (columnName.equals(idField, true))
                continue
            dm[columnName] = columnValue
        }
        return dm
    }

}

internal class JdbcTemplateWrapper(val template: NamedParameterJdbcTemplate,
                                   val sqlStatistics: SqlStatistics)
    : NamedParameterJdbcOperations by template {

    @Throws(DataAccessException::class)
    override fun <T> query(sql: String, paramMap: Map<String, *>, rowMapper: RowMapper<T>): List<T> {
        val start = System.currentTimeMillis()
        val res = template.query(sql, paramMap, rowMapper)
        val end = System.currentTimeMillis()

        sqlStatistics.addSqlStat(sql, paramMap, end - start)

        return res

    }

}