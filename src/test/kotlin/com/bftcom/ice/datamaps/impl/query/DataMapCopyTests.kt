package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.impl.util.*
import com.bftcom.ice.server.*
import com.bftcom.ice.statemachine.*
import org.junit.Test
import javax.annotation.Resource

/**
 * Created by Щукин on 03.11.2017.
 */
open class CopyTests : BaseSpringTests() {

    @Resource
    lateinit var stateMachineService: StateMachineService

    @Resource
    lateinit var dataMapsProjectionUtilService: DataMapsProjectionUtilService

    @Test()
    fun testCopyPerson() {
        //пример создания DataMap с фиелсетами
        val person = Person {
/* TODO:  Person.id - все равно заменяется порядковым номером из seq - разобраться, почему при явном указании
            для оракакла IdGenerationType.IDENTITY он не заменяется и из-за этого валится на констрейнте в Child.person_id,
               а в постгрес IdGenerationType.SEQUENCE и все заменяется на сгенерированный
               (может надо диалект поправить?)
               */
           // it.id = 100L
            it[name] = "Boris"
            it[lastName] = "Schukin"
            it[age] = 38
            //референс
            it[gender] = find_(Gender.filter { name eq "man" })
            //список
            it[childs].add(
                    Child {
                        it[name] = "Sasha"
                    }
            )
        }
        dataService.flush()

        val bs = dataService.find_(Person.slice {
            full()
            filter { lastName eq value("Schukin") }
        })
        bs.printAsJson(true)

        val bs1 = dataService.copy(bs)
        assertNull(bs1.id)
        assertTrue(bs1.isNew())
        assertNotNull(bs1[{ childs }].size == 1)
        assertNull(bs1[{ childs }][0].id)
        assertTrue(bs1[{ childs }][0].isNew())

        bs1["lastName"] = "Schukin2"
        dataService.flush()


        val bs2 = dataService.find_(Person.slice {
            full()
            filter { lastName eq value("Schukin2") }
        })
        bs.printAsJson(true)
        bs2.printAsJson(true)

        assertNotNull(bs2.id)
        assertTrue(bs.id != bs2.id)
        assertNotNull(bs2[{ childs }].size == 1)
        assertNotNull(bs2[{ childs }][0].id)
        assertTrue(bs2[{ childs }][0].id != bs[{ childs }][0].id)
    }


    @Test()
    fun testCopyStateMachine() {
        val sm = buildStateMachine()
        dataService.flush()

        val json = sm.toJson()

        val sm2 = dataMapFromJson(json) as DataMapF<StateMachine>
        assertFalse(sm2.isNew())
        assertTrue(sm2[{ states}].size==3)
        assertTrue(sm2[{ states}].map { it.isNew() }.distinct().size==1)
        assertFalse(sm2[{ states}].map { it.isNew() }.distinct()[0])
        assertTrue(sm2[{ states}].flatMap { it[{ transitions}] }.size==2)
        assertFalse(sm2[{ states}].flatMap { it[{ transitions}] }.map { it.isNew() }.distinct()[0])
        assertTrue(sm2[{ states}].flatMap { it[{ transitions}] }.map { it.isNew() }.distinct().size==1)

        val sm22 = dataService.copy(sm2)
        assertTrue(sm22.isNew())
        assertTrue(sm22[{ states}].size==3)
        assertTrue(sm22[{ states}].map { it.isNew() }.distinct().size==1)
        assertTrue(sm22[{ states}].map { it.isNew() }.distinct()[0])
        assertTrue(sm22[{ states}].flatMap { it[{ transitions}] }.size==2)
        assertTrue(sm22[{ states}].flatMap { it[{ transitions}] }.map { it.isNew() }.distinct()[0])
        assertTrue(sm22[{ states}].flatMap { it[{ transitions}] }.map { it.isNew() }.distinct().size==1)

        dataService.flush()

        assertFalse(sm22.isNew())
        assertTrue(sm22[{ states}].size==3)
        assertTrue(sm22[{ states}].map { it.isNew() }.distinct().size==1)
        assertFalse(sm22[{ states}].map { it.isNew() }.distinct()[0])
        assertTrue(sm22[{ states}].flatMap { it[{ transitions}] }.size==2)
        assertFalse(sm22[{ states}].flatMap { it[{ transitions}] }.map { it.isNew() }.distinct()[0])
        assertTrue(sm22[{ states}].flatMap { it[{ transitions}] }.map { it.isNew() }.distinct().size==1)

        (stateMachineService as CacheClearable).clearCache()

        val sm3 = stateMachineService
                .getStateMachine(sm[{ code }])
        sm3!!.printAsJson()


    }


    @Test
    //Тест на получение проекции, которая получит "полный образ сущности" -
    //все скалярные поля и ссылки и зависимые коллекции.
    // А для зависимых коллекций также получит их зависимые колллекции и так далее рекурсивно.
    fun testFullImageProjectionForEntity() {
        val projection = dataMapsProjectionUtilService
                .getFullImageProjection("stateMachine")
        assertTrue(projection.groups.size==4)

        assertTrue(projection.fields["states"]!=null)
        assertTrue(projection.fields["states"]!!.groups.size==4)
        assertTrue(projection.fields["states"]!!.fields["transitions"]!=null)
    }


    @Test()
    //тест показывающий копирование с использованием проекции полного образа
    fun testCopyStateMachineByFullImageProjectionLoading() {
        val sm = buildStateMachine()
        dataService.flush()

        val sm22 = dataService.copy(StateMachine.entity, sm.id!!)
        sm22.printAsJson(JsonWriteOptions(writeBackRefs = false))

        assertTrue(sm22.isNew())
        assertTrue(sm22[{ StateMachine.states }].size==3)
        assertTrue(sm22[{ StateMachine.states }].map { it.isNew() }.distinct().size==1)
        assertTrue(sm22[{ StateMachine.states }].map { it.isNew() }.distinct()[0])
        assertTrue(sm22[{ StateMachine.states }].flatMap { it[{ State.transitions }] }.size==2)
        assertTrue(sm22[{ StateMachine.states }].flatMap { it[{ State.transitions }] }.map { it.isNew() }.distinct()[0])
        assertTrue(sm22[{ StateMachine.states }].flatMap { it[{ State.transitions }] }.map { it.isNew() }.distinct().size==1)

        dataService.flush()

        assertFalse(sm22.isNew())
        assertTrue(sm22[{ StateMachine.states }].size==3)
        assertTrue(sm22[{ StateMachine.states }].map { it.isNew() }.distinct().size==1)
        assertFalse(sm22[{ StateMachine.states }].map { it.isNew() }.distinct()[0])
        assertTrue(sm22[{ StateMachine.states }].flatMap { it[{ transitions }] }.size==2)
        assertFalse(sm22[{ StateMachine.states }].flatMap { it[{ transitions }] }.map { it.isNew() }.distinct()[0])
        assertTrue(sm22[{ StateMachine.states }].flatMap { it[{ transitions }] }.map { it.isNew() }.distinct().size==1)

    }

    fun buildStateMachine(): DataMapF<StateMachine> {
        val sm = StateMachine.create {
            it[name] = "Третья статусная"
            it[code] = "ThirdSM"

            val one = State.create {
                it[name] = "first3"
                it[code] = "first3"
                it[isStart] = true
            }
            val two = State {
                it[name] = "second3"
                it[code] = "second3"
            }
            val three = State {
                it[name] = "three3"
                it[code] = "three3"
            }

            one[{ transitions }].add(Transition {
                it[name] = "Первый переход 3"
                it[code] = "firstTrans3"
                it[target] = two
                it[{ isDefault }] = true
                it[{ action }] = Action {
                    it[script] = """
                                _entity["description"] = "this is my description #1"
                        """
                }
            })
            two[{ transitions }].add(Transition {
                it[name] = "Второй переход 3"
                it[code] = "secondTrans3"
                it[target] = three
            })

            it[states].add(one)
            it[states].add(two)
            it[states].add(three)
        }
        return sm
    }


}