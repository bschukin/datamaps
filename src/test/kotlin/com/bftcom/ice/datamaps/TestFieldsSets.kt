package com.bftcom.ice.datamaps

object Gender : MFS<Gender>() {
    val id = Field.id()
    val name = Field.string("name")
    val isClassic = Field.boolean("isClassic")

    override val nativeKey: List<Field<*, *>> by lazy{
        listOf(name)
    }
}
typealias GDR = Gender

object City : MFS<City>() {
    val id = Field.id()
    val title = Field.string("title")
}

object Person : MFS<Person>() {
    val id = Field.id()
    val name = Field.string("name")
    val lastName = Field.string("lastName")
    val age = Field.int("age")
    val gender = Field.referenceNN("gender", Gender)
    val childs = Field.list("childs", Child)
    val email = Field.string("email")
    val city = Field.reference("city", City)
    val bio = Field.clob("bio")
    val photo = Field.blob("photo")
    val favoriteGame = Field.reference("favoriteGame", Game)

    override val nativeKey: List<Field<*, *>> by lazy{
        listOf(Gender.name)
    }
}

object Child : MFS<Child>() {

    val id = Field.int("id")
    val name = Field.string("name")
}

typealias WRKR = Person

object Department : MFS<Department>() {
    val id = Field.id()
    val name = Field.stringNN("name")
    val fullName = Field.stringNN("fullName")
    val parent = Field.referenceNN("parent", Department)
    val childs = Field.list("departments", Department)
    val boss = Field.reference("boss", Person)
    val city = Field.reference("city", City)

    init {
        addOption(Tree(parent = parent, childs = childs))
    }
}

object Project : MFS<Project>() {
    val name = Field.string("name")
    val tasks = Field.list("tasks", Task)
}

object Task : MFS<Task>() {
    val name = Field.string("name")
    val checks = Field.list("checklists", Check)
}

typealias TSK = Task

object Check : MFS<Check>("Checklist") {
    val id = Field.id()
    val name = Field.string("name")
    val task = Field.reference("task", Task)
}

object Attach: MFS<Attach>() {
    val id = Field.id()
    val name = Field.string("name")
    val data = Field.jsonbd("data")
    val data2 = Field.jsonObj("data2", AttachData2)

    object AttachData2 : MFS<AttachData2>(options = *arrayOf(Dynamic)) {

        val name = Field.string("name")
        val part = Field.jsonObj("part", AttachData3)

        object AttachData3 : MFS<AttachData3>("AttachData3", options = *arrayOf(Dynamic)) {
            val foo = Field.string("foo")
            val bar = Field.string("bar")
        }
    }
}



object Game: MFS<Game>() {
    val id = Field.stringId()
    val name = Field.string("name")
    val metacriticId = Field.int("metacriticId")
    val episodes = Field.list("gameepisodes")

}

object GameEpisode: MFS<GameEpisode>() {
    val id = Field.stringId()
    val name = Field.string("name")
}

object DateTest: MFS<DateTest>() {
    val id = Field.stringId()
    val timestamp = Field.timestamp("timestamp")
    val tdate = Field.date("tdate")
}

object IgorsMap : MFS<IgorsMap>("Igorsmap", "IgorsMap") {
    val id = Field.id()
    val upperName = Field.string("upperName")
    val data = Field.jsonObj("data", IgorsMap2)
    override val nativeKey: List<Field<*, *>>
        get() = listOf(upperName)

    object IgorsMap2 : MFS<IgorsMap2>(DataMap.DYNAMIC_ENTITY) {

        val innerName = Field.string("innerName")
        val part = Field.jsonObj("part", IgorsMap3)

        object IgorsMap3 : MFS<IgorsMap3>(DataMap.DYNAMIC_ENTITY) {
            val foo = Field.string("foo")
            val bar = Field.string("bar")
        }
    }
}

object FooBar : MFS<FooBar>() {

    val id = Field.int("id")
    val name = Field.string("name")
    val value = Field.int("value")
}