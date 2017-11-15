package com.datamaps

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

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