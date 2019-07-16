package com.bftcom.ice.datamaps.core.util

import org.springframework.stereotype.Service

@Service
class SqlStatistics {


    private val queries = mutableListOf<SqlStat>()

    private val updates = mutableListOf<SqlStat>()

    private val inserts = mutableListOf<SqlStat>()

    private var started = false

    fun addSqlStat(sql: String, params: Map<String, *>, millis: Long) {

        if (started)
            queries.add(SqlStat(sql, params, millis))
    }

    fun addSqlUpdate(sql: String, params: Map<String, *>, millis: Long) {

        if (started) {
            if (sql.startsWith("INSERT", true))
                inserts.add(SqlStat(sql, params, millis))
            else
                updates.add(SqlStat(sql, params, millis))
        }
    }

    fun start() {
        started = true
        queries.clear()
        updates.clear()
        inserts.clear()
    }

    fun restart() {
        start()
    }

    fun stop() {
        started = false
    }

    fun queries() = queries.toList()

    fun lastQuery() = queries.last().sql

    fun lastInsert() = inserts.last().sql

    fun updates() = updates.toList()

    fun inserts() = inserts.toList()
}

data class SqlStat(val sql: String, val params: Map<String, *>, val millis: Long)
