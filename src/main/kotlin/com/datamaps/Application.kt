package com.datamaps

import com.datamaps.services.DbDialect
import com.datamaps.services.getDbDialectByConnection
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.shell.CommandLine
import org.springframework.shell.core.JLineShellComponent
import javax.annotation.Resource




/**
 *
 *
 */
@EnableAutoConfiguration
@SpringBootApplication(scanBasePackages = ["com.datamaps", "org.springframework.shell"])
@ImportResource("app-context.xml")
class KotlinDemoApplication {

    @Resource
    lateinit var jdbcTemplate: JdbcTemplate

    @Bean
    fun jLineShellComponent(): JLineShellComponent {
        return JLineShellComponent()
    }

    @Bean
    fun commandLine(): CommandLine {
        return CommandLine(null, 3000, null)
    }

    @Bean
    fun dbDialect(): DbDialect {
        return getDbDialectByConnection(jdbcTemplate.dataSource.connection)
    }

}

fun main(args: Array<String>) {

    SpringApplication.run(KotlinDemoApplication::class.java, *args)

}

