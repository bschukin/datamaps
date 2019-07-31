package com.bftcom.ice.statemachine

import com.bftcom.ice.datamaps.*

//https://docs.microsoft.com/ru-ru/dotnet/framework/windows-workflow-foundation/state-machine-workflows


interface StateMachineService {
    /***
     * Получить описание статусной модели
     */
    fun getStateMachine(stateMachineCode: String): DataMapF<StateMachine>?

    /***
     * Для типа сущности регистрирует дефолтную статусную модель
     * (чтобы не писать явно руками все время)
     */
    fun registerDefaultStateMachine(entity: String, stateMachineCode: String)

    fun getDefaultStateMachine(entity: String): String?

    /***
     * Инициализировать объект статусной моделью
     * @target - объект, для которого инициализиоуется статусная модель (TODO: вообще говоря этот объект не обязателен)
     * @stateMachineCode - код статусной машины.
     * @saveInTarget - сохранить ли в объекте @target ссылку на stateMachineInctanse
     */
    fun initStateMachineInstance(target: DataMap, stateMachineCode: String)

    /***
     * Инициализировать объект статусной моделью
     */
    fun initStateMachineInstance(target: DataMap, stateMachine: DataMapF<StateMachine>)

    /***
     * Выполнить перевод объекта по указанному переходу
     */
    fun doTransition(dataMap: DataMap, transition: DataMapF<Transition>? = null, flushAfterTransition: Boolean = true)

    /***
     * Выполнить перевод объекта по указанному переходу
     */
    fun doTransitionByCode(dataMap: DataMap, transitionCode: String, flushAfterTransition: Boolean = true)

}


//////////////////////////////////////////////////////////////////
//КЛАССЫ ОПИСЫВАЮЩИЕ СТАТУСНУЮ МОДЕЛЬ
//////////////////////////////////////////////////////////////////

object StatesGroup : FieldSetGroup("states", "Статусы и переходы")

/**
 * Машина состояний (она же статусная модель, она же workflow).
 */
object StateMachine : MappingFieldSet<StateMachine>("StateMachine", "Статусные модели", StatesGroup) {

    val id = Field.id()
    val name = Field.stringNN("name").apply { caption = "Наименование" }
    val code = Field.stringNN("code").apply { caption = "Код" }
    val states = Field.list("states", State).apply { caption = "Состояния" }

    override val nativeKey: List<Field<*, *>> by lazy {
        listOf(code)
    }

    fun DataMapF<StateMachine>.findStartState(): DataMapF<State>? {
        return this[states].find { st -> true == st[State.isStart] }
    }

    fun DataMapF<StateMachine>.findState(code: String): DataMapF<State>? {
        return this[states].find { st -> code == st[State.code] }
    }

    fun fullStateMachine() = StateMachine.slice {
            full()
        states {
            full()
            group {
                +name
            }
            transitions {
                scalars().withRefs().withBlobs()
            }
        }

        }

}


/***
 * Состояние. Он же статус.
 */
object State : MappingFieldSet<State>("State", "Статусы", StatesGroup) {
    val id = Field.id()
    val name = Field.stringNN("name").apply { caption = "Статус" }
    val code = Field.stringNN("code").apply { caption = "Код" }
    val stateMachine = Field.reference("stateMachine", StateMachine).apply { caption = "Статусная модель" }
    val transitions = Field.list("transitions", Transition).apply { caption = "Переходы" }
    val isStart = Field.boolean("isStart").apply { caption = "Начальный" }
    val isFinal = Field.boolean("isFinal").apply { caption = "Конечный" }
    val isExclusiveGateway = Field.boolean("isExclusiveGateway").apply { caption = "Ветвление" }
    val group = Field.referenceNN("group", StateGroup).apply { caption = "Группа" }

    override val nativeKey: List<Field<*, *>> by lazy {
        listOf(code)
    }

    fun DataMapF<State>.getDefaultTransition(): DataMapF<Transition>? {
        if (this[transitions].size == 1)
            return this[transitions][0]

        return this[transitions].find { it[{ isDefault }] == true }
    }
}

/***Категория перехода*/
object StateGroup : MappingFieldSet<StateGroup>("StateGroup", "Группы статуса", StatesGroup) {
    val id = Field.id()
    val name = Field.stringNN("name").apply { caption = "Наименование" }
    val color = Field.stringNN("color").apply { caption = "Цвет" }

    override val nativeKey: List<Field<*, *>> by lazy {
        listOf(name)
    }
}

/***
 * Переход. Он и в африке переход.
 */
object Transition : MappingFieldSet<Transition>("Transition", "Переходы", StatesGroup) {
    val id = Field.id()
    val name = Field.stringNN("name").apply { caption = "Имя перехода" }
    val state = Field.referenceNN("state", State) { caption = "Исходное состояние" }
    val target = Field.referenceNN("target", State) { caption = "Конечное состояние" }
    val isDefault = Field.boolean("isDefault") { caption = "Переход по умолчанию" }
    val code = Field.stringNN("code") { caption = "Код перехода" }

    val action = Field.jsonObj("action", Action) { caption = "Действие" }
    val conditionScript = Field.string("conditionScript") { caption = "Условие перехода" }

    override val nativeKey: List<Field<*, *>> by lazy {
        listOf(name)
    }
}

object Action : MappingFieldSet<Action>("Action", "Действие", StatesGroup, Dynamic) {

    val isService = Field.booleanNN("isService") { defaultValue = { true }; caption = "Сервис/Сценарий" }

    val className = Field.string("className") { caption = "Класс" }
    val method = Field.string("method") { caption = "Метод" }

    val script = Field.string("script") { caption = "Скрипт" }
}

//////////////////////////////////////////////////////////////////
//ЭКЗЕМПЛЯРЫ СТАТУСНОЙ МОДЕЛИ
//////////////////////////////////////////////////////////////////
val stateMachineInstanceField = Field.referenceNN("stateMachineInstance", StateMachineInstance)
val stateField = Field.reference("state", State)

object StateMachineInstance : MappingFieldSet<StateMachineInstance>("StateMachineInstance", "Экзэмпляры статусной модели", StatesGroup) {

    val id = Field.id()
    val stateMachine = Field.referenceNN("stateMachine", StateMachine) { caption = "Статусная модель" }
    val state = Field.referenceNN("state", State) { caption = "Статус" }
    val created = Field.timestampNN("created") { caption = "Создан" }
    val changed = Field.timestamp("changed") { caption = "Изменен" }

    //дефолтный слайс
    fun defaultSlice(): DataProjectionF<StateMachineInstance> {
        val sl = slice {
            stateMachine {
                scalars()
            }
            state {
                scalars()
                transitions {
                    +name
                    +isDefault
                }
            }
        }
        return sl
    }

    fun shortenSlice() = slice {
            stateMachine {
                +code
            }
        state {
            +code
        }
    }
}