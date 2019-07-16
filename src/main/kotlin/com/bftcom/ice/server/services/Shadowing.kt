package com.bftcom.ice.server.services

import com.bftcom.ice.common.maps.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

//
@Service
class ShadowService {

    companion object {
        internal lateinit var instance: ShadowService
    }

    @Autowired
    lateinit var dataService: DataService


    @PostConstruct
    fun init() {
        instance = this
    }


    fun shadow(dataMap: DataMap): DataMap {
        val fieldSet = FieldSetRepo.fieldSet(dataMap.entity)
        val projection = on(dataMap.entity)

        //ну сделаем проекцию на первый уровень
        //по хорошему надо бы рекурсивно
        projection.scalars()
        dataMap.map.forEach {
            projection.with(it.key)
        }

        var dm: DataMap? = null
        if (fieldSet.nativeKey.size > 0) {
            fieldSet.nativeKey.forEach {
                val afield = it
                projection.filter {
                    f(afield) eq value(dataMap[afield.n])
                }
            }

            dm = dataService.find(projection)
            if (dm != null && dataMap.id==null) {
              //надо идентификаторы надйенного положить в dataMap
                //чтобы нормально ссылки проставились
                dataMap.id = dm.id
                dataMap.persisted()
            }
        }
        if (dm == null) {
            dm = dataService.insert(dataMap)
        }
        return dm
    }


}

fun <T : FieldSet> MappingFieldSet<T>.shadow(body: T.(DataMapF<T>) -> Unit): DataMapF<T> {

    return buildInSilence {
        val c = DataMapF(this, this.entity, null,  true)
        body(this as T, c as (DataMapF<T>))
        val res = ShadowService.instance.shadow(c)
        res as (DataMapF<T>)
    }

}