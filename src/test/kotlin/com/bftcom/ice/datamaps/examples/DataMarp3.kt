package com.bftcom.ice.datamaps.examples

import com.bftcom.ice.datamaps.f
import com.bftcom.ice.datamaps.on
import com.bftcom.ice.datamaps.slice
import com.bftcom.ice.datamaps.BaseSpringTests
import com.bftcom.ice.datamaps.Department
import org.junit.Ignore
import org.junit.Test


/***
 * Usages of  static fieldsets
 */
open class DataMarp3 : BaseSpringTests() {


    @Test @Ignore//TODO сделать подержку "деревяннх" свойств
    fun basicProjectionUses() {

        //пример запроса
        val datamap =
                    dataService.find(on(Department.entity)
                            .with {
                                slice(Department.parent)
                                        .field(Department.name)
                                        .field(Department.fullName)
                            }
                            .with {
                                slice(Department.childs)
                                        .field(Department.name)
                                        .with {
                                            slice(Department.parent)
                                                    .field(Department.name)
                                        }
                            }
                            .filter {
                                f(Department.childs().fullName) eq f(Department.parent().name)
                            })!!


        //пробивает по проперте "parent.fullname" новое свойство
        datamap[Department.name] = "яволь"
        datamap[Department.parent().fullName] = "zer gut"

    }


    @Test()
    fun basicDataMapsUses2() {

        val dtp = Department.create()

        with(Department)
        {
            //смотрите на удобные присвоения
            dtp[name] = "БИС"
            dtp[fullName] = "ЗАО БИС"
            dtp[parent] = create {it[name]= "Россия"}
            dtp[parent().fullName] = "Российская федерация"
            /*dtp[childs].add(create { it[fullName] = "Аналитический отдел" })
            dtp[childs].forEach {
                it[parent] = dtp[parent]
            }*/

            //смотрите на то что из изндексатора приходит объект нужного типа

            var str1 = dtp[name].capitalize() //вернулась строка и мы ее дернули
            var str2 = dtp[parent][fullName].capitalize() //из парента вернулся датамап и мы дернули на нем индексатор, а там опять строка
            var str3 = dtp[parent().fullName].capitalize() //тоже самое, но с нестед пропертей

            //var long1 = dtp[childs][0][id].dec() //пример с коллекциями (доступ по индексу 0) и показано что id - лонг

        }
    }
}


