package com.bftcom.ice.datamaps.core.query

import com.bftcom.ice.datamaps.Field
import com.bftcom.ice.datamaps.MFS
import com.bftcom.ice.datamaps.BaseSpringTests
import com.bftcom.ice.datamaps.Game
import com.bftcom.ice.datamaps.GameEpisode
import com.bftcom.ice.datamaps.Person
import com.bftcom.ice.datamaps.core.util.printAsJson
import org.junit.Assert
import org.junit.Test

/**
 * Created by Щукин on 03.11.2017.
 */
open class IdTests : BaseSpringTests() {


    object UUID1 : MFS<UUID1>("UUID1"){
        val id = Field.guid()
        val name = Field.string("name")
    }

    @Test
    fun testUUID() {
        val u = UUID1 {
            it[name] = "hellow"
        }

        assertNotNull(u.id)
        dataService.flush()

        val f = dataService.find_(UUID1.withId(u.id) )
        println(f)
        if (isOracle() || isFireBird()) {
            assertTrue(f.id == u.id.toString())
        } else {
            assertTrue(f.id == u.id)
        }

        assertFalse(f.id === u.id)

        dataService.delete(f)
    }


    @Test
    fun testQueryObjectsWithStringId() {
        //I достанем и проверим dm со строковым id
        val game = dataService.find_(Game.filter { id eq "HEROES" })
        Assert.assertEquals(game.id, "HEROES")

        //II достанем и проверим еще и с коллекцией
        val game2 = dataService.find_(
                Game.slice {
                    full()
                    filter { id eq "HEROES" }
                })
        game2.printAsJson()
        Assert.assertEquals(game2.id, "HEROES")
        Assert.assertEquals(game2[Game.episodes].size,2)

        //II достанем другой объект со ссылкой на строковый референс
        val gazman = dataService.find_(
                Person.slice {
                    withRefs()
                    favoriteGame{full()}
                    filter { Person.favoriteGame().id eq "SAPIOR" }
                })
        Assert.assertNotNull(gazman[Person.favoriteGame])
        Assert.assertTrue(gazman[Person.favoriteGame().episodes].size==1)
        Assert.assertTrue(gazman[Person.favoriteGame().metacriticId] == 666)

        gazman.printAsJson()

    }

    @Test
    fun testChangeObjectsWithStringId() {

        //II достанем и проверим еще и с коллекцией
        val game = dataService.find_(
                Game.slice {
                    full()
                    filter { id eq "HEROES" }
                })
        game[Game.name] = "Герои"
        game[Game.episodes].add(
                GameEpisode.create {
                    it[id] = "HEROES4"
                }
        )
        dataService.flush()

        val game2 = dataService.find_(
                Game.slice {
                    full()
                    filter { id eq "HEROES" }
                })
        game2.printAsJson(true)
        Assert.assertEquals(game2[Game.episodes].size,3)

    }


}