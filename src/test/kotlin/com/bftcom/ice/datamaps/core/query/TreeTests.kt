package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.TreeQueryOptions
import com.bftcom.ice.datamaps.BaseSpringTests
import com.bftcom.ice.datamaps.Department
import com.bftcom.ice.datamaps.assertBodyEquals
import com.bftcom.ice.datamaps.core.util.IfSpringProfileActive
import com.bftcom.ice.datamaps.core.util.printAsJson
import org.junit.Test
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */
open class TreeTests : BaseSpringTests() {

    @Resource
    private lateinit var queryRecursiveBuilder: QueryRecursiveBuilder

    @Test
    @IfSpringProfileActive("postgresql")
    fun testQueryChild() {

        val dep = dataService.find_(Department.filter { id eq 2 })


        val childs = dataService.loadChilds(listOf(dep))

        dep.printAsJson()

        assertTrue(childs.size == 2)
        assertTrue(dep[{ Department.childs }].size == 2)
        assertTrue(dep[{ Department.childs }].find { it.id == 4 }!![Department.childs].size == 1)

    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testQueryParents() {

        //тест 1
        val deps = dataService.findAll(Department.filter { id IN (listOf(3, 4)) })

        val parents = dataServiceExtd.loadParents(deps, null,
                TreeQueryOptions(buildHierarchy = false, mergeIntoSourceList = false, includeLevel0 = false))

        assertTrue(parents.size == 2)


        //тест 2
        val parents2 = dataServiceExtd.loadParents(deps, null,
                TreeQueryOptions(buildHierarchy = true, mergeIntoSourceList = false, includeLevel0 = true))

        assertTrue(parents2.size == 2)

        //тест 3
        val dep = dataService.find_(Department.filter { id IN (listOf(5)) })


        dep.with {
            assertTrue(dep[parent]==null)

            dataService.loadParents(listOf(dep), null,
                    TreeQueryOptions(buildHierarchy = true, mergeIntoSourceList = true, includeLevel0 = true))

            dep.printAsJson()

            assertNotNull(dep[parent])
            assertNotNull(dep[parent().parent])
            assertNotNull(dep[parent().parent().parent])
            assertNull(dep[parent().parent().parent().parent])
        }

    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testChildHierarchyQueryInProjection() {

        val dep =
                dataService.find_(Department.slice {
                    +name
                    childs {
                        option().recursive()
                        +name
                    }
                    filter { id eq 2 }
                })



        dep.printAsJson(false)

        dep.with {
            assertTrue(dep[childs].size == 2)
            assertTrue(dep[childs].find { it.id == 4 }!![childs].size == 1)
        }

    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testParentHierarchyQueryInProjection() {

        val dep =
                dataService.find_(Department.slice {
                    +name
                    parent {
                        option().recursive()
                        +name
                    }
                    filter { id eq 5 }
                })


        dep.printAsJson(false)

        assertNotNull(dep[Department.parent])
        assertNotNull(dep[Department.parent().parent])
        assertNotNull(dep[Department.parent().parent().parent])
        assertNull(dep[Department.parent().parent().parent().parent])

    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testRecursiveQueryChildBuilding() {

        val dp = Department.slice {
            filter { id eq 2 }
        }


        val q = queryRecursiveBuilder.createRecursiveFindQuery(dp, "parent", true, false)

        println(q.sql)
        assertBodyEquals(q.sql,
                """
                   WITH RECURSIVE __R__ AS (SELECT
                     department1."id"  AS  id1,  department1."parent_id"  AS  parent_id1,  (1) AS  __level
                FROM department department1
                WHERE department1."id" = :param0

                    UNION

                SELECT
                     department1."id"  AS  id1,  department1."parent_id"  AS  parent_id1,  (__level+1) AS  __level
                FROM department department1
                JOIN __R__ r1 ON department1.parent_id = r1.id1)

                    SELECT * FROM __R__
                    """)
    }

    @Test
    @IfSpringProfileActive("postgresql")
    fun testRecursiveQuerParentBuilding() {

        val dp = Department.slice {
            filter { id eq 2 }
        }


        val q = queryRecursiveBuilder.createRecursiveFindQuery(dp, "parent", false, false)

        println(q.sql)

        assertBodyEquals(q.sql,
                """
                   WITH RECURSIVE __R__ AS (SELECT
	 department1."id"  AS  id1,  department1."parent_id"  AS  parent_id1,  (0) AS  __level
FROM department department1
WHERE department1."id" = :param0

	UNION

SELECT
	 department1."id"  AS  id1,  department1."parent_id"  AS  parent_id1,  (__level-1) AS  __level
FROM department department1
JOIN __R__ r1 ON department1.id = r1.parent_id1)

	SELECT * FROM __R__ where __level < 0
                    """)
    }
}