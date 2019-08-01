package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.DataService
import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.datamaps.assertBodyEquals
import com.bftcom.ice.datamaps.core.mappings.CamelUnderscoreNameMappingsStrategy
import com.bftcom.ice.datamaps.core.mappings.NameMappingsStrategy
import com.bftcom.ice.datamaps.core.util.printAsJson
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests
import javax.annotation.PostConstruct
import kotlin.test.assertEquals



@SpringBootTest(classes = [ RemappingWithTablePrefixTests.TestConfig2::class ])
@ContextConfiguration("classpath:test-app-context.xml", inheritLocations = false)
@EnableAutoConfiguration
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
open class RemappingWithTablePrefixTests : AbstractTransactionalJUnit4SpringContextTests() {

    @Autowired
    internal lateinit var queryBuilder: QueryBuilder

    @Autowired
    lateinit var dataService: DataService

    @Autowired
    lateinit var nameMappingsStrategy: NameMappingsStrategy

    @Configuration
    @SpringBootApplication(scanBasePackages = ["com.bftcom.ice.server"])
    open class TestConfig2 {
        @Bean
        open fun nameMappingsStrategy(): NameMappingsStrategy {
            return CamelUnderscoreNameMappingsStrategy()
        }
    }

    @PostConstruct
    fun init() {
        (nameMappingsStrategy as CamelUnderscoreNameMappingsStrategy).tableNamePrefix = "PREF_"
    }

    //Тест на ремап таблицы
    @Test
    fun testRemapTable01() {
        val dp2 = queryBuilder.createQueryByDataProjection(Leg.on())
        assertBodyEquals("""
            SELECT
        	  PREF_LEG1."ID"  AS  ID1,  PREF_LEG1."NAME"  AS  NAME1
            FROM PREF_LEG PREF_LEG1
            """, dp2.sql)

        val dp = queryBuilder.createQueryByDataProjection(
                Animal.on().withCollections()
        )
        println(dp.sql)
        assertBodyEquals("""
            SELECT
	            PREF_ANIMAL1."ID"  AS  ID1,  PREF_ANIMAL1."NAME"  AS  NAME1,  PREF_LEG1."ID"  AS  ID2,
                PREF_LEG1."NAME"  AS  NAME2
            FROM PREF_ANIMAL PREF_ANIMAL1
            LEFT JOIN PREF_LEG  PREF_LEG1 ON PREF_ANIMAL1."ID"=PREF_LEG1."ANIMAL_ID"
            """, dp.sql)

        //ради интереса убедимся, что sql-запрос пройдет на настоящей базе
        val res = dataService.findAll(Animal.on().withCollections())

        res.printAsJson()

        assertEquals(res[0][Animal.legs].size, 2)
    }

}

/**
 * Created by Щукин on 03.11.2017.
 */
object Animal : MFS<Animal>("Animal") {
    val id = Field.id()
    val name = Field.string("name")
    val legs = Field.list("legs", Leg)
}

object Leg : MFS<Leg>("Leg")
{
    val name = Field.string("name")
}