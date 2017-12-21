package com.datamaps.services

import org.springframework.stereotype.Service

@Service
class SqlStatistics {


    private val queries = mutableListOf<SqlStat>()

    private var started = false

    fun addSqlStat(sql: String, params: Map<String, *>, millis: Long) {

        if (started)
            queries.add(SqlStat(sql, params, millis))
    }

    fun start() {
        started = true
        queries.clear()
    }

    fun restart() {
        start()
    }

    fun stop() {
        started = false
    }

    fun queries() = queries.toMutableList()
}

data class SqlStat(val sql: String, val params: Map<String, *>, val millis: Long)
