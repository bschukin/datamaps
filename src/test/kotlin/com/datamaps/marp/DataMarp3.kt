package com.datamaps.marp

import com.datamaps.BaseSpringTests
import com.datamaps.DTP
import com.datamaps.Department
import com.datamaps.mappings.f
import com.datamaps.mappings.on
import com.datamaps.mappings.slice
import com.datamaps.maps.datamap
import org.testng.annotations.Test

@Suppress("USELESS_IS_CHECK")
class DataMarp3 : BaseSpringTests() {




    @Test(invocationCount = 0)
    fun basicProjectionUses() {

        //пример запроса
        val datamap =

                with(Department)
                {
                    dataService.find(on(entity)
                            .with {
                                slice(parent)
                                        .field(name)
                                        .field(fullName)
                            }
                            .with {
                                slice(childs)
                                        .field(name)
                                        .with {
                                            slice(parent)
                                                    .field(name)
                                        }
                            }
                            .filter {
                                f(childs().fullName) eq f(parent().name)
                            })!!
                }

        //пробивает по проперте "parent.fullname" новое свойство
        datamap[DTP.name] = "яволь"
        datamap[DTP.parent().fullName] = "zer gut"

    }


    @Test(invocationCount = 1)
    fun basicDataMapsUses2() {

        val dtp = datamap(Department)

        with(Department)
        {
            //смотрите на удобные присвоения
            dtp[name] = "БИС"
            dtp[fullName] = "ЗАО БИС"
            dtp[parent().name] = "Российская федерация"
            dtp[childs].forEach {
                it[parent] = dtp[parent]
            }

            //смотрите на то что из изндексатора приходит объект нужного типа

            var str1 = dtp[name].capitalize() //вернулась строка и мы ее дернули
            var str2 = dtp[parent][fullName].capitalize() //из парента вернулся датамап и мы дернули на нем индексатор, а там опять строка
            var str3 = dtp[parent().fullName].capitalize() //тоже самое, но с нестед пропертей
            var long1 = dtp[childs][0][id].dec() //пример с коллекциями (доступ по индексу 0) и показано что id - лонг

        }
    }
}


