package com.datamaps

import com.datamaps.mappings.AsIsNameMappingsStrategy
import com.datamaps.mappings.CamelUnderscoreNameMappingsStrategy
import com.datamaps.mappings.NameMappingsStrategy
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




@EnableAutoConfiguration
@SpringBootApplication(scanBasePackages = ["com.datamaps", "org.springframework.shell"])
@ImportResource("app-context.xml")
class KotlinDemoApplication {

    @Resource
    lateinit var jdbcTemplate: JdbcTemplate

    @Bean
    fun nameMappingsStrategy(): NameMappingsStrategy {
        return CamelUnderscoreNameMappingsStrategy()
    }

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

@EnableAutoConfiguration
@SpringBootApplication(scanBasePackages = ["com.datamaps", "org.springframework.shell"])
@ImportResource("app-context.xml")
class ServiceDeskApplication:KotlinDemoApplication() {

    @Bean
    override fun nameMappingsStrategy(): NameMappingsStrategy {
        return AsIsNameMappingsStrategy()
    }

}


fun main(args: Array<String>) {

    val ctx = SpringApplication.run(ServiceDeskApplication::class.java, *args)

    val cli = CliService()
    ctx.autowireCapableBeanFactory.autowireBean(cli)
    cli.init()


}

