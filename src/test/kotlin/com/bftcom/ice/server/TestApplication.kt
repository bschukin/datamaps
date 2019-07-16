package com.bftcom.ice.server

import com.bftcom.ice.datamaps.core.mappings.AsIsNameMappingsStrategy
import com.bftcom.ice.datamaps.core.mappings.NameMappingsStrategy
import com.bftcom.ice.datamaps.core.dialects.DbDialect
import com.bftcom.ice.datamaps.core.util.SqlStatistics
import com.bftcom.ice.datamaps.core.dialects.getDbDialectByConnection
import com.bftcom.ice.datamaps.core.util.JdbcTemplateWrapper
import com.bftcom.ice.datamaps.core.util.NamedJdbcTemplateWrapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.annotation.Resource


@SpringBootApplication
open class TestApplication {

    @Resource
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var sqlStatistics: SqlStatistics

    @Bean
    open fun nameMappingsStrategy(): NameMappingsStrategy {
        return AsIsNameMappingsStrategy()
    }

    @Bean
    open fun namedPrameterJdbcTemplate(): NamedParameterJdbcOperations {
        val t = JdbcTemplateWrapper(jdbcTemplate, sqlStatistics)
        return NamedJdbcTemplateWrapper(NamedParameterJdbcTemplate(t), sqlStatistics)
    }

    @Bean
    open fun dbDialect(): DbDialect {
        return getDbDialectByConnection(jdbcTemplate)
    }
}