package com.datamaps.maps

import com.datamaps.services.DeltaStore

//не является частью АПИ
//коллекция датамапов
//нужна для передачи сообщений об изменении
internal class DataList(val list: ArrayList<DataMap>,
                        val parent: DataMap, val property: String) : MutableList<DataMap> by list {

    override fun add(element: DataMap): Boolean {
        DeltaStore.listAdd(parent, element, property)
        return list.add(element)
    }

    override fun remove(element: DataMap): Boolean {
        DeltaStore.listRemove(parent, element, property)
        return list.remove(element)
    }

    fun addSilent(element: DataMap): Boolean {
        return list.add(element)
    }

}