package com.bftcom.ice.datamaps

import com.bftcom.ice.datamaps.core.delta.*
import com.bftcom.ice.datamaps.core.mappings.DataMapping
import com.bftcom.ice.datamaps.core.util.JsonWriteOptions
import com.bftcom.ice.datamaps.core.util.toJson
import com.bftcom.ice.datamaps.misc.GUID
import com.bftcom.ice.datamaps.misc.makeSure
import org.jetbrains.annotations.ApiStatus
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.util.*
import java.util.stream.Stream

/**
 * Created by Щукин on 27.10.2017.
 */
interface DataService {

    fun get(entityName: String, id: Any?): DataMap?

    fun findAll(entityName: String): List<DataMap>

    fun <T : FieldSet> findAll(dp: DataProjectionF<T>): List<DataMapF<T>>

    fun unionAll(up: UnionProjection): List<DataMap>

    fun <T : FieldSet> find(dp: DataProjectionF<T>): DataMapF<T>?

    fun <T : FieldSet> find_(dp: DataProjectionF<T>): DataMapF<T>


    fun count(dp: DataProjection): Int

    fun upgrade(maps: List<DataMap>, slice: DataProjection, returnLoadedParts: Boolean = false): List<DataMap>

    fun delete(datamap: DataMap): Boolean

    fun deleteAll(dp: DataProjection)

    fun sqlToFlatMaps(entity: String, sql: String, params: Map<String, Any> = mapOf(), idColumn: String = "ID"): List<DataMap>

    fun sqlToFlatMap(entity: String, sql: String, params: Map<String, Any> = mapOf(), idColumn: String = "ID"): DataMap?

    fun flush()

    fun insert(dataMap: DataMap, preInsertAction: ((DataMap) -> Unit)? = null, runTriggers:Boolean = true): DataMap

    fun <T : FieldSet>  copy(source: DataMapF<T>): DataMapF<T>

    fun  copy(entity: String, id:Any): DataMap

    fun springBeanMethodCall(beanClass: String, method: String, vararg args: Any?): Any?
}


@ApiStatus.Experimental
interface DataServiceExtd : DataService {
    fun getDataMapping(name: String): DataMapping

    fun deleteAll(entity: String)

    /**
     * Быстрая массовая вставка записей в базу.
     * Айдишник должен быть либо уже в датамапе, либо присваиваться при инсерте как identity (serial)
     */
    fun bulkInsert(list: List<DataMap>, runBeingOperations: Boolean = true, presInsertAction: ((DataMap) -> Unit)? = null)

    /**
     * Быстрая массовая вставка записей в базу.
     * Айдишник должен быть либо уже в датамапе, либо присваиваться при инсерте как identity (serial)
     */
    fun bulkInsert(stream: Stream<DataMap>, entity: String, runBeingOperations: Boolean = true, presInsertAction: ((DataMap) -> Unit)? = null)


    fun <T : FieldSet> loadChilds(list: List<DataMapF<T>>, projection: DataProjection? = null,
                                  options: TreeQueryOptions = TreeQueryOptions()): List<DataMapF<T>>

    fun <T : FieldSet> loadParents(list: List<DataMapF<T>>, projection: DataProjection? = null,
                                   options: TreeQueryOptions = TreeQueryOptions()): List<DataMapF<T>>
}

class TreeQueryOptions(var parentProperty: String? = null,
                       var childProperty: String? = null,
                       var depth: Int? = -1,
                       var mergeIntoSourceList: Boolean = true,
                       var buildHierarchy: Boolean = true,
                       var  includeLevel0:Boolean = false )

/***
 * Контекст триггера (события ЖЦ сущности)
 */
interface TriggerContext {

    /***
     * Все изменения по записи, собранные в виде датамапа.
     * (в карте присутствуют >>только<<< изменененные свойства).
     * (Json-поля будут собраны в виде интегрального нового состояния динамического датамапа)
     * Карту можно изменять (в том числе, любой уровень вложенности json-полей)
     */
    val delta: DataMap


    /***
     * Предыдущее состояние записи (точнее, то состояние, которое сейчас находится в БД).
     * Достается отдельным запросом (повнимательней).
     * DataMap в состоянии readonly  - менять  нельзя
     */
    fun old(): DataMap?

    /***
     * Новое состояние записи (здесь можно получиьт все свойства, в отличие от delta).
     * Если изменения произведены на сервере  - то new() - ничего не стоит.
     * Если изменения сделаны на клиенте - то new() = find().withRefs().withBlobs() U  deltas
     */
    fun new(): DataMap

    val isClientChange: Boolean


    var cancel:Boolean

}

/***
 * Интерфейс сервиса жизненного цикла сущностей заданного типа.
 *
 * Позволяет выполнять определенные действиях на событиях жизненого цикла сущности:
 * инсерте, апдейте, удалении
 *
 * Сервис должен быть зарегистрирован как спринговый бин.
 * При старте будет создан реестр подобных сервисов и использоваться на операции флаша изменений в БД
 */
interface DataMapTriggers {

    companion object {
        val allEntities = listOf("allEntities")
    }

    /***
     * Целевые типы сущностей для которых будет вызываться данный сервис.
     * !!Если надо подписаться на изменения во всех сущностях (например всякие журналы)
     * - необходимо вернуть ссылку на allEntities (см. выше)
     */
    val targetEntities: List<String>

    /***
     * Приоритет сервиса. Для каждой сущноссти может быть создано несколько реестров.
     * Приоритет задает порядок их вызова - от нижнего к верхнему
     */
    fun getPriority(): Int = 0

    /**
     * Вызывается перед операциями инсертов. В этот момент можно еще менять delta
     */
    fun beforeInsert(event: TriggerContext) {}


    /**
     * Вызывается перед операциями инсертов. В этот момент можно еще менять delta
     */
    fun beforeUpdate(event: TriggerContext) {}

    /**
     * Вызывается перед операциями удаления.
     */
    fun beforeDelete(event: TriggerContext) {}

    /***
     * Вызывается после операции апдейта.
     *  Измененение записи НЕ приведет к новым апдейтам
     */
    fun afterUpdate(event: TriggerContext) {}

    /***
     * Вызывается сразу после создания новой записи.
     * Можно увидеть созданный id
     * Измененение записи приведет к апдейтам.
     */
    fun afterInsert(event: TriggerContext) {}

    /**
     * Вызывается сразу после удаления записи.
     */
    fun afterDelete(event: TriggerContext) {}


}




fun <T> inSilence(builder: () -> T): T {
    DeltaStore.silence()
    try {
        return builder()
    } finally {
        DeltaStore.noise()
    }

}


