package com.bftcom.ice.datamaps.examples

import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.BaseSpringTests
import com.bftcom.ice.datamaps.Child
import com.bftcom.ice.datamaps.Gender
import com.bftcom.ice.datamaps.Person
import org.junit.Assert
import org.junit.Test


/***
 * Базовые примеры на слайсы и проекции
 */
open class DataMarp : BaseSpringTests() {


    @Test
    fun dataMapsBasicOperations() {

        //пример создания DataMap
        val person = DataMap("Person", 100L)
        person["name"] = "Boris"
        person["lastName"] = "Schukin"
        person["age"] = 38

        //добавляем ссылку M-1 в Person: находим в базе экземпляр справочника "пол"
        person["gender"] = find_(on("Gender").where("{{name}} = 'man' "))

        //добавляем в список
        person.list("childs").add(
                //также показан конструктор принимающий пары значений
                DataMap("Child", 101L, mapOf("name" to "Sasha", "age" to 13))
        )

        //примеры чтения по индексатору мапа
        val age = person["age"]

        //использование оператора "()"
        // доступ к свойству гендер справочника "gender"
        val genderName = person("gender")!!["name"]

        //использование функции "list"
        // доступ к свойству гендер справочника "gender"
        val childName = person.list("childs")[0]["name"]

        print(age.toString() + genderName + childName)

    }

    @Test
    fun dataMapsBasicOperations2() {

        //пример создания DataMap с фиелсетами
        val person = Person.create {
            it.id = 100L
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


        //примеры чтения по индексатору мапа
        val age1 = person[Person.age]

        //использование оператора "()"
        // доступ к свойству гендер справочника "gender"
        val genderName = person[Person.gender][Gender.name]

        //использование функции "list"
        // доступ к свойству гендер справочника "gender"
        val childName = person[Person.childs][0][Person.name]

        print(age1.toString() + genderName + childName)

        //2я версия
        with(Person)
        {
            val age2 = person[age]

            //использование оператора "()"
            // доступ к свойству гендер справочника "gender"
            val genderName2 = person[gender][Gender.name]

            // использование nested свойства
            val childName2 = person[childs[0].name]
        }

        //находим всех персон
        var persons = dataService.findAll(Projection("Person"))

    }


    @Test
    fun dataMapsAndFieldSets() {
        //create API
        val m1 = Gender {
            it[id] = 100L
            it[name] = "ccc"
        }

        val myGender = Gender.create {
            it[id] = 100L
            it[name] = "был такой"
        }

        val person = Person.create {
            it[id] = 100L
            it[name] = "some hero"
            it[gender] = myGender
            it[gender().name] = "стал другой"
        }

        //update API
        //часть вторая - апдейты
        update(Gender, m1).with {
            it[name] = "zzz"
        }

        Person.update(person) {
            it[name] = "some zero"
            it[gender] = m1
            it[gender().name] = "совсем иной"
        }
    }

    @Test
    fun testInsertAndUpdate0() {

        val w0 = DataMap("Person", isNew = true)
        w0["name"] = "Fiedor"
        w0["email"] = "dostoevsky@yandex.ru"

        dataService.flush()

        //ищем нашу организацию
        val w1 = dataService.find_(on(Person).full()
                .filter { Person.name eq "Fiedor" })

        //апдейтим
        w1["name"] = "Fiedor D."
        w1["gender"] = dataService.find(on(Gender).where("{{name}}='name'"))

        dataService.flush()
    }

    @Test
    fun testInsertAndUpdate() {

        Person.create {
            it[name] = "Fiedor"
            it[email] = "dostoevsky@yandex.ru"
        }

        dataService.flush()

        val w = dataService.find_(Person.filter { name eq "Fiedor" })

        Person.update(w) {
            it[name] = "Fiedor Dostoevsky"
            it[gender] = dataService.find(on(Gender).where("{{name}}='man'"))
        }

        dataService.flush()
    }


    @Test
    fun basicProjectionUses() {

        //простой пример
        val gender = dataService.find_(
                on(Gender).id(2L)
        )

        gender[Gender.name] = "men"
        dataService.flush()

        //более сложный пример
        val person = dataService.find_(
                on(Person)
                        .id(1L)
                        .field(Person.name)
                        .with {
                            slice(Person.gender)
                                    .field(Gender.name)
                        }
        )

        if (person("gender")!!["gender"] != gender["gender"])
            person["gender"] = gender
        //не флашим, дожидаемся окончания транзакции


        //пример с деревом
        val dp2 = on("Department")
                .with {
                    slice("parent")
                            .field("n")
                            .field("n")
                }
                .with {
                    slice("childs")
                            .field("n")
                            .with {
                                slice("parent")
                                        .field("n")
                            }
                }
    }


    @Test
    fun basicProjectionSlices01() {

        var dp = on("City")
                .withRefs()
                .field("n")
                .with {
                    slice("person")
                            .scalars().withRefs()
                }
                .field("gender")

        //коллекции
        dp = on("Project")
                .withCollections()
                .with {
                    slice("Tasks")
                            .scalars().withRefs()
                }

        //алиасы
        dp = on("Project")
                .full()
                .alias("JP")
                .with {
                    slice("Tasks")
                            .alias("JT")
                            .scalars().withRefs()
                }
    }


    /**
     * Пример "плоского" API проекций. Просто перечисляются поля через точку.
     * Только эти поля и будут вытащены
     */
    @Test
    fun flatProjectionsExamples() {

        //через строковые имена (оператор !)
        var dp = on(Person).with(
                !Person.name,
                !Person.city().title,
                !Person.gender().name
        ).filter(f(Person.gender().name) eq "M")

        //через поля фиелдсетов
        var dp2 = on(Person).with(
                Person.name,
                Person.city().title,
                Person.gender().name
        ).filter(f(Person.gender().name) eq "M")

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
    fun basicFiltersApiMethod() {

        val dp = on("Department")
                .withRefs()
                .field("n")
                .with {
                    slice("boss").alias("W")
                            .scalars().withRefs()
                }
                .field("city")
                .filter({
                    {
                        {
                            (f("W.city.id") eq f("city.id"))
                        } or
                                { f("W.n") eq value("nanyr") }
                    } and
                            {
                                f("W.email") eq value("gazman@google.com") or
                                        { f("n") eq value("zzz") }
                            }
                })
                .limit(100).offset(100)
    }

    @Test
    fun basicFiltersBindingMethod() {

        //без алиасов
        var dp = on("Department")
                .withRefs()
                .field("n")
                .with {
                    slice("boss")
                            .scalars().withRefs()
                }
                .field("city")
                .where("""
                    (
                         {{boss.city.id}} = {{city.id}}
                        or
                         {{boss.n}} = :param0
                    ) and
                    (
                        {{boss.email}} = :param1 or
                        {{n}} = 'zzz'
                    )
                """)
                .param("param0", "nanyr")
                .param("param1", "gazman@google.com")


        //с алиасом
        dp = on("Department")
                .withRefs()
                .field("n")
                .with {
                    slice("boss").alias("W")
                            .scalars().withRefs()
                }
                .field("city")
                .where("""
                    (
                         {{W.city.id}} = {{city.id}}
                        or
                         {{W.n}} = :param0
                    ) and
                    (
                        {{W.email}} = :param1 or
                        {{n}} = 'zzz'
                    )
                """)
    }

    @Test
    fun testExistsInQueryWithBindinds() {

        //пример использования байндинга объектов графа запроса в WHERE-выражении

        val p = (on("Project")
                .with {
                    slice("Tasks") //загружаем коллекцию тасков
                            .with {
                                slice("checklists") //загружаем коллекцию чеков
                                        .scalars()
                            }
                }
                .where("{{name}} = 'QDP' AND " +
                        "EXISTS (SELECT j.id FROM CHECKLIST j WHERE j.TASK_ID = {{Tasks.id}})")
                )

        val projects = dataService.findAll(p)
        println(projects)
    }


    @Test
    fun basicFormulas() {

        //формула
        val gender = dataService.find(on("Gender")
                .formula("caption", """
                    case when {{id}}=1 then 'Ж'
                         when {{id}}=2 then 'М'
                         else 'О' end
                """)
                .filter(f("caption") eq "Ж"))!!

        println(gender["caption"])

        //lateral
        val dp = on("Project")
                .lateral("tasks", """
                    (select string_agg(t.n, ';') as tasks1, count(*) as qty1
                            from task t
                            where t.project_id= {{id}}
                            ) tasks on true
                    """,
                        "tasks1" to "tasks", "qty1" to "qty")
                .where("{{tasks.tasks1}} like '%001%'")

    }

    @Test
    fun basicUpgrades() {

        //1 грузим  проекты без коллекций
        val projects = dataService.findAll(
                on("Project")
                        .scalars()
                        .where("{{name}} = 'QDP'")
        )

        //2 догружаем коллекции
        dataService.upgrade(projects, projection()
                .with {
                    slice("Tasks") //загружаем коллекуию тасков
                            .full() //все поля - следвателоьно и вложенная коллекция чеклистов полетит
                })


        //use complex indexator
        Assert.assertTrue(projects[0].nested("Tasks[0].checklists[0].name") == "foo check")
    }

    /***
     * Пример с отдельными запросами для вложенных коллекций
     *
     * мне короче удалсоь сделать так, что будет три запроса ровно:
    - первый на проекты.
    - второй на все задачи выбраных проектов
    - третий - на все чеклисты всех выбранных задач
     */
    @Test

    fun testSelectCollectionWithSeparateSelect() {

        //1 грузим  проекты без коллекций
        val projects = dataService
                .findAll(on("Project")
                        .scalars()
                        .with {
                            slice("tasks") //загружаем коллекуию тасков
                                    .scalars().asSelect()
                                    .with {
                                        slice("checklists") //загружаем коллекуию чеклистов
                                                .scalars().asSelect()
                                    }
                        }
                        .where("{{name}} = :qdp").param("qdp", "QDP")
                )


        println(projects)
    }


    @Test
            /**
             * пример создания датамапа на основе выполнения SQL.
             * маппинг в адатамап осуществляет по именам колонок (columnLabel) представленным в резулт-сете.
             * */
    fun testSqlToMapMethod() {
        var res = dataService.sqlToFlatMap(Person.entity,
                "select w.id , w.name as noname, g.name, g.id as gid from person w " +
                        "left join  gender g on g.id = w.gender_id " +
                        "where g.name = :_name",
                mapOf("_name" to "man"))!!

        println(res)

        """
            {
                "entity": "Person",
                "id": 2,
                "GID": "2",
                "NAME": "man",
                "NONAME": "John Lennon"
        }"""

    }


    /**
     * Примеры выгрузки в json
     */
    @Test
    fun testPrintToJson() {
        var list = dataService.findAll(on("Project")
                .full()
                .with {
                    slice("Tasks")
                            .scalars().withRefs()
                })

        //1 тест на  структуру по которой построится запрос
        val res = StringBuilder()
        list.forEach { e -> res.append(e.toString()) }
        list.forEach { e -> println(e) }

    }

}