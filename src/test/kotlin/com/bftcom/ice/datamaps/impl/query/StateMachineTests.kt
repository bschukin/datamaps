package com.bftcom.ice.datamaps.impl.query

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.server.BaseSpringTests
import com.bftcom.ice.server.assertEqIgnoreCase
import com.bftcom.ice.datamaps.impl.query.StateMachineTests.GuessNumber.tryCount
import com.bftcom.ice.datamaps.impl.util.CacheClearable
import com.bftcom.ice.datamaps.impl.util.printAsJson
import com.bftcom.ice.statemachine.*
import org.junit.Ignore
import org.junit.Test
import org.springframework.stereotype.Service
import javax.annotation.Resource
import kotlin.test.assertTrue

/**
 * Created by Щукин on 03.11.2017.
 */

open class StateMachineTests : BaseSpringTests() {

    @Resource
    lateinit var stateMachineService: StateMachineService


    @Test
    fun testStateMachineCrud() {
        buildStateMachine()

        dataService.flush()
        val sm2 = dataService.find_(StateMachine.slice {
            full()
            states {
                full()
                transitions {
                    withRefs()
                }
            }
            filter { f(code) eq value("FirstSM") }
        })
        sm2.printAsJson(true)


        //выбрать все транзишины принадлежащие нашей статусной модели
        val trans = dataService.findAll(
                Transition
                        .slice {
                            +state
                        }.filter {
                            f(Transition.state().stateMachine().id) eq value(sm2.id)
                        })
        println(trans)
        assertTrue(trans.size == 2)

        //выбрать все статусы принадлежащие нашей статусной модели
        //кроме статуса яляещегося исходным для выбранного транзишина
        val states = dataService.findAll(State.filter {
            { f(stateMachine().id) eq value(sm2.id) } and {
                f(id) neq value(trans[0][Transition.state].id)
            }
        })
        println(states)
        assertTrue(states.size == 2)
    }


    @Test
    fun testStateMachineService() {

        (stateMachineService as CacheClearable).clearCache()

        assertNull(stateMachineService.getStateMachine("FirstSM"))

        buildStateMachine()

        dataService.flush()

        assertNotNull(stateMachineService.getStateMachine("FirstSM"))
    }


    @Test
    fun testStateMachineInstance() {
        buildStateMachine()
        dataService.flush()
        assertNotNull(stateMachineService.getStateMachine("FirstSM"))

        stateMachineService.registerDefaultStateMachine(Dogovor.entity, "FirstSM")

        //создаем объект
        val d = Dogovor {
            it[description] = "some dogovor"
        }

        //инициализируем статусную машину
        //(теперь явно вызывать не надо - вызовется в StateMachineInstanceTriggers)
        //stateMachineService.initStateMachineInstance(d, "FirstSM")

        //сохраняем
        dataService.flush()

        //получаем договор
        val dogovor = dataService.find_(
                Dogovor.slice {
                    withId(d.id)
                    full()
                    stateMachineInstance {
                        stateMachine {
                            scalars()
                        }
                        state {
                            scalars()
                            +transitions
                        }
                    }
                })

        dogovor.printAsJson(false)

        //сейча договор в статусе first
        assertEqIgnoreCase("first", dogovor[{ stateMachineInstance().state().code }])

        //переводим во второй статус (без явного указания перехода - само должно определиться)
        stateMachineService.doTransition(dogovor)
        assertEqIgnoreCase("second", dogovor[{ stateMachineInstance().state().code }])

        //переводим в третий статус с явным указанием перехода
        stateMachineService.doTransition(dogovor, dogovor[{ stateMachineInstance().state().transitions }][0])
        assertEqIgnoreCase("third", dogovor[{ stateMachineInstance().state().code }])

        // на переходе выполнился акшен  DogovorAction. applyFinalAction
        //смотрим применился ли он
        assertTrue { Dogovor.counter > 0 }
    }

    /***
     * Тест на "маппинг и сопровожение@  в сущность под статусной моделью - поля @state@ и поля "StateMachineInstance"
     */
    @Test
    fun testStateMachineInstanceWithStateFieldAndStateMachineInstance() {
        buildStateMachine()
        dataService.flush()
        assertNotNull(stateMachineService.getStateMachine("FirstSM"))

        stateMachineService.registerDefaultStateMachine(Dogovor2.entity, "FirstSM")

        //создаем объект
        val d = Dogovor2 {
            it[description] = "some dogovor"
        }

        //инициализируем статусную машину
        //(теперь явно вызывать не надо - вызовется в StateMachineInstanceTriggers)
        //stateMachineService.initStateMachineInstance(d, "FirstSM")

        //сохраняем
        dataService.flush()

        //получаем договор
        val dogovor2 = dataService.find_(
                Dogovor2.slice {
                    withId(d.id)
                    full()
                    stateMachineInstance {
                        stateMachine {
                            scalars()
                        }
                        state {
                           +code
                        }
                    }
                    state{
                        +code
                        +group
                    }
                })

        dogovor2.printAsJson(false)


        //сейча договор в статусе first
        assertEqIgnoreCase("first", dogovor2[{ stateMachineInstance().state().code }])
        assertTrue(dogovor2[{ stateMachineInstance().state().id}]==dogovor2[{ state().id}])

        //переводим во второй статус (без явного указания перехода - само должно определиться)
        stateMachineService.doTransitionByCode(dogovor2, "first")
        assertEqIgnoreCase("second", dogovor2[{ stateMachineInstance().state().code }])
        assertTrue(dogovor2[{ stateMachineInstance().state().id}]==dogovor2[{ state().id}])

        //переводим в третий статус с явным указанием перехода
        stateMachineService.doTransition(dogovor2, dogovor2[{ stateMachineInstance().state().transitions }][0])
        assertEqIgnoreCase("third", dogovor2[{ stateMachineInstance().state().code }])
        assertTrue(dogovor2[{ stateMachineInstance().state().id}]==dogovor2[{ state().id}])

        // на переходе выполнился акшен  DogovorAction. applyFinalAction
        //смотрим применился ли он
        assertTrue { Dogovor.counter > 0 }
    }

    /***
     * Тест на "маппинг и сопровожение@  в сущность под статусной моделью - поля @state@ и поля "StateMachineInstance"
     */
    @Test
    fun testStateMachineInstanceWithStateFieldAndStateMachineInstance2() {
        buildStateMachine()
        dataService.flush()
        assertNotNull(stateMachineService.getStateMachine("FirstSM"))

        stateMachineService.registerDefaultStateMachine(Dogovor2.entity, "FirstSM")

        //создаем объект
        val d = Dogovor2 {
            it[description] = "some dogovor"
        }

        //инициализируем статусную машину
        //(теперь явно вызывать не надо - вызовется в StateMachineInstanceTriggers)
        //stateMachineService.initStateMachineInstance(d, "FirstSM")

        //сохраняем
        dataService.flush()

        //получаем договор
        val dogovor2 = dataService.find_(
                Dogovor2.slice {
                    withId(d.id)
                    state{
                        +code
                    }
                })

        dogovor2.printAsJson(false)


        //сейча договор в статусе first
        assertEqIgnoreCase("first", dogovor2[{ state().code }])

        //переводим во второй статус (без явного указания перехода - само должно определиться)
        stateMachineService.doTransitionByCode(dogovor2, "first")
        assertEqIgnoreCase("second", dogovor2[{ state().code }])

        //переводим в третий статус с явным указанием перехода
        stateMachineService.doTransitionByCode(dogovor2, "second")
        assertEqIgnoreCase("third", dogovor2[{ state().code }])


    }

    /***
     * Тест на "маппинг и сопровожение@  в сущность под статусной моделью - поля @state@
     */
    @Test
    fun testStateMachineInstanceWithStateFieldOnly() {
        buildStateMachine()
        dataService.flush()
        assertNotNull(stateMachineService.getStateMachine("FirstSM"))

        stateMachineService.registerDefaultStateMachine(Dogovor3.entity, "FirstSM")

        //создаем объект
        val d = Dogovor3 {
            it[description] = "some dogovor"
        }

        //инициализируем статусную машину
        //(теперь явно вызывать не надо - вызовется в StateMachineInstanceTriggers)
        //stateMachineService.initStateMachineInstance(d, "FirstSM")

        //сохраняем
        dataService.flush()

        //получаем договор
        val dogovor3 = dataService.find_(
                Dogovor3.slice {
                    withId(d.id)
                    full()
                    state{
                        +code
                    }
                })

        dogovor3.printAsJson(false)


        //сейча договор в статусе first
        assertEqIgnoreCase("first", dogovor3[{state().code }])
        assertNull(dogovor3[stateMachineInstanceField])

        //переводим во второй статус (без явного указания перехода - само должно определиться)
        stateMachineService.doTransitionByCode(dogovor3, "first")
        assertEqIgnoreCase("second", dogovor3[{state().code }])
        assertNull(dogovor3[stateMachineInstanceField])

        //переводим в третий статус с явным указанием перехода
        stateMachineService.doTransitionByCode(dogovor3, "second")
        assertEqIgnoreCase("third", dogovor3[{ state().code }])
        assertNull(dogovor3[stateMachineInstanceField])

    }

    //@Test
    fun testStateMachineInstanceCascadeDelete() {

        buildStateMachine()
        dataService.flush()
        assertNotNull(stateMachineService.getStateMachine("FirstSM"))

        stateMachineService.registerDefaultStateMachine(Dogovor.entity, "FirstSM")

        //создаем объект
        val d = Dogovor {
            it[description] = "some dogovor"
        }

        //сохраняем
        dataService.flush()

        //получаем договор
        val dogovor = dataService.find_(
                Dogovor.slice {
                    withId(d.id)
                    full()
                    +stateMachineInstance
                })

        dogovor.printAsJson(false)

        //сейча договор в статусе first
        assertNotNull(dogovor[{ stateMachineInstance }])

        val simId = dogovor[{ stateMachineInstance }]!!.id

        dataService.delete(dogovor)
        dataService.flush()

        assertNull(dataService.find(Dogovor.withId(dogovor.id)))
        assertNull(dataService.find(StateMachineInstance.withId(simId)))
    }


    @Test
    fun testStateMachineInstanceWithTransitionCode() {
        buildStateMachine()
        dataService.flush()
        assertNotNull(stateMachineService.getStateMachine("FirstSM"))

        stateMachineService.registerDefaultStateMachine(Dogovor.entity, "FirstSM")

        //создаем объект
        val d = Dogovor {
            it[description] = "some dogovor"
        }

        //инициализируем статусную машину
        //(теперь явно вызывать не надо - вызовется в StateMachineInstanceTriggers)
        //stateMachineService.initStateMachineInstance(d, "FirstSM")

        //сохраняем
        dataService.flush()

        //получаем договор
        val dogovor = dataService.find_(
                Dogovor.slice {
                    withId(d.id)
                    full()
                    stateMachineInstance by StateMachineInstance.shortenSlice()
                })

        dogovor.printAsJson(false)

        //сейчас договор в статусе first
        assertEqIgnoreCase("first", dogovor[{ stateMachineInstance().state().code }])

        //переводим во второй статус с явным указанием перехода по коду
        stateMachineService.doTransitionByCode(dogovor, "first")
        assertEqIgnoreCase("second", dogovor[{ stateMachineInstance().state().code }])

        //переводим в третий статус с явным указанием перехода по коду
        stateMachineService.doTransitionByCode(dogovor, "second")
        assertEqIgnoreCase("third", dogovor[{ stateMachineInstance().state().code }])

        // на переходе выполнился акшен DogovorAction. applyFinalAction
        //смотрим применился ли он
        assertTrue { Dogovor.counter > 0 }
    }

    @Test
    @Ignore
    fun testStateMachineScriptAction() {
        buildStateMachine2()
        dataService.flush()
        assertNotNull(stateMachineService.getStateMachine("SecondSM"))

        //создаем объект
        val d = Dogovor {
            it[description] = "some dogovor"
        }

        //инициализируем статусную машину
        stateMachineService.initStateMachineInstance(d, "SecondSM")

        //сохраняем
        dataService.flush()

        //получаем договор
        val dogovor = dataService.find_(
                Dogovor.slice {
                    withId(d.id)
                    full()
                    stateMachineInstance by StateMachineInstance.defaultSlice()
                })

        dogovor.printAsJson(false)

        //сейча договор в статусе first2
        assertEqIgnoreCase("first2", dogovor[{ stateMachineInstance().state().code }])

        //переводим во второй статус (без явного указания перехода - само должно определиться)
        stateMachineService.doTransition(dogovor)
        assertEqIgnoreCase("second2", dogovor[{ stateMachineInstance().state().code }])

        //после перехода, меняется description


        // на переходе выполнился акшен  DogovorAction. applyFinalAction
        //смотрим применился ли он
        assertTrue { dogovor[{ description }] == "this is my description #1" }
    }

    object Dogovor : MappingFieldSet<Dogovor>() {

        val id = Field.id()
        val stateMachineInstance = stateMachineInstanceField.apply { thisJoinColumn =  "STATE_MACHINE_INSTANCE_ID"}
        val description = Field.string("description")
        val created = Field.string("created")

        var counter = 0
    }

    object Dogovor2 : MappingFieldSet<Dogovor2>() {

        val id = Field.id()
        val stateMachineInstance = stateMachineInstanceField.apply { thisJoinColumn =  "STATE_MACHINE_INSTANCE_ID"}
        val state = stateField.apply { thisJoinColumn =  "STATE_ID"}
        val description = Field.string("description")
        val created = Field.string("created")

    }

    object Dogovor3 : MappingFieldSet<Dogovor3>() {
        val id = Field.id()
        val state = stateField.apply { thisJoinColumn =  "STATE_ID"}
        val description = Field.string("description")
        val created = Field.string("created")
    }

    @Service
    class DogovorAction {
        fun applyFinalAction(dataMap: DataMap, transition: DataMapF<Transition>) {
            assertTrue { transition[{ name }] == "Второй переход" }
            Dogovor.counter++
        }
    }

    private fun buildStateMachine(): Any? {
        val sm = StateMachine {
            it[name] = "Первая статусная"
            it[code] = "FirstSM"

            val one = State.create {
                it[name] = "first"
                it[code] = "first"
                it[isStart] = true
            }
            val two = State.create {
                it[name] = "second"
                it[code] = "second"
            }
            val three = State.create {
                it[name] = "third"
                it[code] = "third"
                it[isFinal] = true
            }

            one[{ transitions }].add(Transition.create {
                it[name] = "Первый переход"
                it[code] = "first"
                it[target] = two
            })

            two[{ transitions }].add(Transition.create {
                it[name] = "Второй переход"
                it[code] = "second"
                it[target] = three
                it[{ action }] = Action {
                    it[className] = DogovorAction::class.java.name
                    it[method] = DogovorAction::applyFinalAction.name
                }
            })

            it[states].add(one)
            it[states].add(two)
            it[states].add(three)
        }
        return sm.id
    }

    private fun buildStateMachine2() {
        val sm = StateMachine.create {
            it[name] = "Вторая статусная"
            it[code] = "SecondSM"

            val one = State.create {
                it[name] = "first2"
                it[code] = "first2"
                it[isStart] = true
            }
            val two = State.create {
                it[name] = "second2"
                it[code] = "second2"
            }

            one[{ transitions }].add(Transition.create {
                it[name] = "Первый переход 2"
                it[code] = "first2"
                it[target] = two
                it[{ isDefault }] = true
                it[{ action }] = Action {
                    it[script] = """
                                _entity["description"] = "this is my description #1"
                        """
                }
            })


            it[states].add(one)
            it[states].add(two)
        }
    }

    @Test
    @Ignore
    fun testStateMachineInstanceExclusiveGateway() {
        buildGuessStateMachine()
        dataService.flush()
        assertNotNull(stateMachineService.getStateMachine("Guess"))

        stateMachineService.registerDefaultStateMachine(GuessNumber.entity, "Guess")

        //создаем объект
        val g = GuessNumber {
            it[tryCount] = 777
        }

        //сохраняем
        dataService.flush()

        //получаем угадываемое число
        val guessNumber = dataService.find_(
                GuessNumber.slice {
                    withId(g.id)
                    full()
                    stateMachineInstance by StateMachineInstance.shortenSlice()
                })

        guessNumber.printAsJson(false)

        //начальный статус isNotGuessed - "Число не угадано"
        assertEqIgnoreCase("isNotGuessed", guessNumber[{ stateMachineInstance().state().code }])

        //переводим во второй статус - exclusiveGateway; проверяем, что статус вернулся на исходное состояние
        stateMachineService.doTransition(guessNumber)
        assertEqIgnoreCase("isNotGuessed", guessNumber[{ stateMachineInstance().state().code }])

        //обновляем значение поля попытка; проверяем что статус вернулся на исходное состояние
        guessNumber[{ tryCount }] = 888
        stateMachineService.doTransition(guessNumber)
        assertEqIgnoreCase("isNotGuessed", guessNumber[{ stateMachineInstance().state().code }])

        //обновляем значение поля попытка; проверяем что статус перешел в конечное состояние
        guessNumber[tryCount] = 555
        stateMachineService.doTransition(guessNumber)
        assertEqIgnoreCase("guessed", guessNumber[{ stateMachineInstance().state().code }])
    }

    object GuessNumber : MappingFieldSet<GuessNumber>("GuessNumber") {
        val id = Field.id()
        val stateMachineInstance = stateMachineInstanceField.apply { thisJoinColumn = "STATE_MACHINE_INSTANCE_ID" }
        var tryCount = Field.intNN("tryCount")
    }

    private fun buildGuessStateMachine() {
        StateMachine.create {
            it[name] = "Угадайка"
            it[code] = "Guess"

            val one = State {
                it[name] = "Число не угадано"
                it[code] = "isNotGuessed"
                it[isStart] = true
            }
            val two = State {
                it[name] = "Попытка угадать"
                it[code] = "tryToGuess"
                it[isExclusiveGateway] = true
            }
            val three = State {
                it[name] = "Число угадано"
                it[code] = "guessed"
                it[isFinal] = true
            }

            one[{ transitions }].add(Transition.create {
                it[name] = "Пробую угадать"
                it[code] = "tryToGuess"
                it[target] = two
            })

            two[{ transitions }].addAll(listOf(
                    Transition {
                        it[name] = "Да, угадал"
                        it[code] = "guessed"
                        it[target] = three
                        it[conditionScript] = """
                                _entity["tryCount"] == 555
                        """
                    },
                    Transition {
                        it[name] = "Нет, не угадал"
                        it[code] = "isNotGuessed"
                        it[target] = one
                        it[conditionScript] = """
                                _entity["tryCount"] != 555
                        """
                    })
            )

            it[states].add(one)
            it[states].add(two)
            it[states].add(three)
        }
    }
}