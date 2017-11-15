package com.datamaps.services

import com.datamaps.maps.DataMap
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSet

/**
 * Created by Щукин on 03.11.2017.
 */
@Service
class QueryExecutor {

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate


    fun findAll(q:SqlQueryContext): List<DataMap>
    {
        var mc = MappingContext(q)
        jdbcTemplate.query(q.sql, { resultSet, i ->
            run {
                mapRow(resultSet, q, mc)
            }
        })

        return mc.result()
    }

    fun executeSingle(q:SqlQueryContext): DataMap
    {
       TODO("NOT IMPLE")
    }


    fun mapRow(resultSet: ResultSet, q:SqlQueryContext, mc:MappingContext)
    {
        q.qr.columnAliases.values.forEach { col->
                q.qr.columnMappers[col]!!.invoke(mc, resultSet)
        }
        mc.clear()
    }

}