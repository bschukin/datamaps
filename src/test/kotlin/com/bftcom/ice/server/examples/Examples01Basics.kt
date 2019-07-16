package com.bftcom.ice.server.examples

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.server.*
import com.bftcom.ice.datamaps.impl.util.printAsJson
import org.junit.Test




/***
 * Примеры на работу с датамапсами и проекциями
 * (Fieldset-API)
 */
open class Examples01 : BaseSpringTests() {


    @Test
    //базовые манипуляции с датамапами:
    // создание, запись, чтение,
    //+ распечатка содержимого
    fun exampleDataMapsBasicOperations() {

        //пример создания DataMap по Person
        val person = Person {
            it[name] = "Boris"
            it[lastName] = "Schukin"
            it[age] = 38
            //референс
            it[gender] = find_(Gender.filter { name eq "man" })
            //список
            it[childs].add(
                    Child.create {
                        it[name] = "Sasha"
                        it[age] = 13
                    }
            )
        }


        //примеры чтения по индексатору
        val age1 = person[Person.age] //"простой" индексатор
        val age2 = person[{ age }]        //лямбда- индексатор
        val age3 = person["age"]        //cтроковый индексатор

        assertTrue(age1 == age2)
        assertTrue(age2 == age3)


        // примеры чтения nested-полей
        val genderName = person[Person.gender().name] //"простой" индексатор
        val genderName2 = person[{ gender().name }] //лямбда- индексатор
        val genderName3 = person["gender.name"] //строковый индексатор

        assertTrue(genderName == genderName2)
        assertTrue(genderName2 == genderName3)

        //работа с внутренними списками
        val childName = person[Person.childs][0][Person.name]   //простые индексаторы
        val childName1 = person["childs[0].name"]   //строковый индексатор

        assertTrue(childName == childName1)


        //запись
        person[{ age }] = 66
        person[Person.gender().name] = "Male"
        person["childs[0].name"] = "Саша"

        //запись с использованием функции with
        person.with {
            it[name] = "some zero"
            it[age] = 67
            it[childs][0][name] = "Саша"
        }

        //распечатаем персону в консоль в виде json
        person.printAsJson(writeSystemProps = true)

        //распечатаем персону в консоль в виде дерева (не json)
        println(person.dataMapToString())
    }

    @Test
    fun testDataMapFieldLamdaAccessors() {

        val res = dataService.findAll(
                Person.slice {
                    full()
                })

        res.forEach {
            //печатаем персону
            println(it[{ city }])

            //можно и так
            println(it[{ city().title }])

            //или так
            println(it[{ gender }][{ name }])
        }
    }

    @Test
    //примеры использования API-филедсетов для создания
    //и изменения датамапов
    fun exampleFieldSetsCreateAndUpdateFunctions() {

        //create API
        //1) функция Create
        val m1 = Gender.create {
            it[id] = 100L
            it[name] = "ccc"
        }

        //2) функция Create может быть вызвана как оператор Invoke
        val myGender = Gender {
            it[id] = 100L
            it[name] = "был такой"
        }

        val person = Person {
            it[id] = 100L
            it[name] = "some hero"
            it[gender] = myGender
            it[gender().name] = "стал другой"
        }

        //update API
        //часть вторая - апдейты

        //способ 1 - рекомендуемый
        person.with {
            it[name] = "some zero"
            it[gender] = m1
            it[gender().name] = "совсем иной"
        }

        //способ 2
        Gender.update(myGender) {
            it[name] = "hllo man"
        }
    }

    @Test
    fun exampleBasicInsertAndUpdate() {

        val p = Person {
            it[name] = "Fiedor"
            it[email] = "dostoevsky@yandex.ru"
        }

        dataService.flush()

        assertNotNull(p.id)

        val p1 = dataService.find_(Person.filter { name eq "Fiedor" })

        p1.with {
            it[name] = "Fiedor Dostoevsky"
            it[gender] = dataService.find((Gender).where("{{name}}='man'"))
        }

        dataService.flush()
    }


    @Test
    fun exampleBasicProjectionUses() {

        //простой пример
        val gender = find_(
                Gender.withId(2L)
        )

        //более сложный пример: слайс, в котором указываем поля которые необхоимо выгрузить
        val person = find_(
                Person.slice {
                    withId(1L)
                    +name
                    gender {
                        +name
                    }
                }
        )

        //еще пример
        val dp = Department.slice {
            parent{

            }
            childs {
                +name
                +fullName
            }
        }
        println(dp)
    }

    @Test
    fun exampleProjectionUsesWithGroups() {

        //I. принципы
        //выгрузка карты City , включая скаляры, ссылки, коллекции
        val dp = City.withId(1L)
                .scalars().withRefs().withCollections()

        //scalars необязательно (эта проекция равна предыдущей)
        val dp1 = City.withId(1L)
                .withRefs().withCollections()

        //скаляры, ссылки, коллекции - это все что есть у объекта.
        //поэтому эта проекция эквивалентна двум предыдущим
        val dp2 = City.withId(1L)
                .full()


        //II. примеры использования групп на разных уровнях проекции
        val dp3 = Project.slice {
            withRefs()              //заказываем все ссылки
            tasks {
                // и коллекцию тасков
                withRefs()          //у таска забираем все ссылки
                checks {
                    //и коллекцию чеков
                    withRefs().withBlobs()  //у чеков мы просим все ссылки и все блобы
                }
            }
        }

        //алиасы (алиасы понадобятся для использования в фильтрах например)
        val dp4 = Project.slice {
            full()
            alias("JP")
            tasks {
                alias("JT")
                scalars().withRefs()
            }
        }
    }

    /**
     * Пример "плоского" API проекций. Просто перечисляются поля через точку.
     * Только эти поля и будут вытащены
     */
    @Test
    fun exampleFlatProjectionsExamples() {

        val dp = Person.on().with(
                Person.name,
                Person.age,
                Person.city().title,
                Person.gender().name
        ).filter(f(Person.gender().name) eq "M")

        println(dp)

    }

    @Test
    fun exampleFiltersApiMethod() {

        //самое простое
        val dp =
                Department.filter {
                    name eq "Департамент имущественных отношений"
                }

        //тоже самое, но с функциями f/value и цепочкой фильтров (обединение по and)
        val dp1 =
                Department
                        .filter {
                            f(name) eq value("Департамент имущественных отношений")
                        }.filter {
                            f("name") eq "Департамент имущественных отношений"
                        }

        // + OR
        val dp2 =
                Department.slice {
                    withRefs()
                    withCollections()

                    filter {
                        { name ilike value("%имущественных отношений") } or
                                { name like value("%проектного%") }
                    }
                }

        // + AND, not null
        val dp3 =
                Department.slice {
                    full()

                    filter {
                        { name IS NULL } and
                                { boss ISNOT NULL }
                    }
                }

        // + использование алиасов
        val dp4 =
                Department.slice {
                    +name
                    boss {
                        alias("B")
                    }
                    filter {
                        f("B.city.id") eq f(city().id)
                    }
                }

    }

    @Test
    //where позволяет писать более сложные поисковые выражения.
    //в комплекте идет система квери-байндинга полей внутри where к алиасам стороящегося sql-выражения
    fun exampleWhereApiMethods() {

        val dp =
                Department.slice {
                    +name
                    +boss
                }
                        .where("""
                            (
                                {{name}} = 'zzz'                        --department.name      -->DERARTMENT1.NAME
                                {{boss.email}} = :param1 or             --department.boss.email -->BOSS1.EMAIL
                            )
                            AND
                            (
                                 {{boss.city.id}} = {{city.id}}         --department.boss.city.id   -->CITY1.ID , CITY2.ID
                                or
                                 {{boss.name}} = :param0                --department.boss.name -->BOSS1.NAME
                            )

                        """)
                        .param("param0", "nanyr")
                        .param("param1", "gazman@google.com")


        //c использованием алиасов
        val dp2 =
                Department.slice {
                    +name
                    boss {
                        city {
                            alias("CC1")
                        }
                    }
                    city {
                        alias("CC2")
                    }
                    where("{{CC1.id}} = {{CC2.id}}")
                }

        //Exists
        val p = Project.slice {
            tasks {
                +checks
            }

            where("{{name}} = 'QDP' AND " +
                    "EXISTS (SELECT j.id FROM CHECKLIST j WHERE j.TASK_ID = {{Tasks.id}})")
        }
    }


    @Test
    //Установка порядка сортировки , лимита и оффсета
    fun exampleLimitOffsetOrdersApiMethods() {
        val p = Project
                .slice {
                    tasks {
                        +checks
                    }

                }
                .order(f(Project.name), f(Project.tasks().name), f(Project.tasks().checks().id))
                .limit(100).offset(500)
    }







}