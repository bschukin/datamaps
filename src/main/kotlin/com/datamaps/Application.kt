package com.datamaps

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ImportResource

/**
 *
 *
 */

@SpringBootApplication(scanBasePackages = arrayOf("com.datamaps"))
//@ImportResource("classpath:app-context.xml")
class KotlinDemoApplication {

    fun main(args: Array<String>) {
        SpringApplication.run(KotlinDemoApplication::class.java, *args)
    }
}