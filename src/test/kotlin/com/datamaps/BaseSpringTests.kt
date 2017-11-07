package com.datamaps

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests


/**
 * Created by Щукин on 01.11.2017.
 */


@SpringBootTest(
        classes = arrayOf(KotlinDemoApplication::class),
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
//@EnableAutoConfiguration()
@ContextConfiguration("classpath:test-app-context.xml")
class BaseSpringTests : AbstractTransactionalTestNGSpringContextTests() {





}