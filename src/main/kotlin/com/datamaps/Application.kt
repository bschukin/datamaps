package com.datamaps

import com.datamaps.services.DbDialect
import com.datamaps.services.getDbDialectByConnection
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import javax.annotation.Resource

/**
 *
 *
 */

@SpringBootApplication(scanBasePackages = arrayOf("com.datamaps"))
//@ImportResource("classpath:app-context.xml")
class KotlinDemoApplication {

    @Resource
    lateinit var jdbcTemplate: JdbcTemplate

    @Bean
    fun dbDialect(): DbDialect {
        return getDbDialectByConnection(jdbcTemplate.dataSource.connection)
    }

    fun main(args: Array<String>) {
        SpringApplication.run(KotlinDemoApplication::class.java, *args)
    }
}