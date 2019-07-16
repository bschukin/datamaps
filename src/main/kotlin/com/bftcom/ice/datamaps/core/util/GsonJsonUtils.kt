package com.bftcom.ice.datamaps.core.util


import com.bftcom.ice.datamaps.*
import com.bftcom.ice.datamaps.DataMapF.Companion.DYNAMIC_HOLDER
import com.bftcom.ice.datamaps.DataMapF.Companion.JSON_PATH
import com.bftcom.ice.datamaps.core.delta.DeltaStore
import com.bftcom.ice.datamaps.core.util.SynthethicIdUtils.newSyntheticId
import com.bftcom.ice.datamaps.misc.*
import com.bftcom.ice.datamaps.misc.Date
import com.google.gson.*
import com.google.gson.stream.JsonReader
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.io.*
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*
import java.util.Spliterator.CONCURRENT
import java.util.Spliterator.IMMUTABLE
import java.util.stream.Stream
import java.util.stream.StreamSupport


//todo добавить: writeHeaderProps - entity + id. writeеTechProps - (свойства начинающиеся с __)
data class JsonWriteOptions(val writeSystemProps: Boolean = true,
                            val writeTableEntityAsHeaders: Boolean = false,
                            private val writeEntityName: Boolean = false,
                            private val writeBackRefs: Boolean = true,
                            val writeJsonTechProps: Boolean = true) {

    fun writeEntityName() = writeEntityName || writeSystemProps
    fun notWriteBackRefs() = !writeSystemProps || !writeBackRefs

}


fun DataMap.printAsJson(writeSystemProps: Boolean = true) {
    println(toJson(writeSystemProps))
}

fun DataMap.printAsJson(options: JsonWriteOptions) {
    println(toJson(options))
}

fun List<DataMap>.printAsJson(writeSystemProps: Boolean = true) {
    println(this.map { it.toJson(writeSystemProps) }.joinToString("\r\n"))
}

fun DataMap.toJson(writeSystemProps: Boolean = true): String {

    return this.toJson(JsonWriteOptions(writeSystemProps))
}

fun Any.toJson(options: JsonWriteOptions): String? {
    return when (this) {
        is List<*> -> (this as List<DataMap>).toJson(options)
        is DataMap -> this.toJson(options)
        this -> null
        else -> TODO()
    }
}

fun DataMap.toJson(options: JsonWriteOptions): String {
    val gson = GsonBuilder()
            .registerTypeAdapter(DataMapF::class.java, DMSerializer(options))
            .setPrettyPrinting()
            .serializeNulls()
            .create()

    return gson.toJson(this)
}

fun List<DataMap>.toJson(writeSystemProps: Boolean = true): String {
    val gson = GsonBuilder()
            .registerTypeAdapter(List::class.java, DMListSerializer(JsonWriteOptions(writeSystemProps)))
            .setPrettyPrinting()
            .serializeNulls()
            .create()

    return gson.toJson(this, List::class.java)
}

fun dataMapFromJson(json: String): DataMap {
    return gsonDataMapDeserializer.fromJson(json, DataMapF::class.java)
}

fun <T : MappingFieldSet<T>> dataMapFromJson(json: String, fieldSet: MappingFieldSet<T>?): DataMapF<T> {
    val gson = GsonBuilder()
            .registerTypeAdapter(DataMapF::class.java, DmJsonDeserializerF(fieldSet))
            .create()
    return gson.fromJson(json, DataMapF::class.java) as DataMapF<T>
}

fun dataMapFromJsonFile(file: File): DataMap {
    return dataMapFromJson(file.readText())
}

fun dataMapsFromJson(json: String?): List<DataMap> {
    if (json == null)
        return emptyList()

    val gson = GsonBuilder()
            .registerTypeAdapter(List::class.java, DmListJsonDeserializer())
            .create()
    return gson.fromJson(json, List::class.java) as List<DataMap>
}


fun dataMapsFromStreammedJson(ins: InputStream): List<DataMap> {

    ins.use {
        val reader = JsonReader(InputStreamReader(ins, "UTF-8"))
        val maps = ArrayList<DataMap>()
        reader.beginArray()
        while (reader.hasNext()) {
            val dm: DataMap = gsonDataMapDeserializer.fromJson(reader, DataMapF::class.java)
            maps.add(dm)
        }
        reader.endArray()
        reader.close()
        return maps
    }
}

fun dataMapStreamFromJson(ins: InputStream): Stream<DataMap> {

    class StreamIterator(val reader: JsonReader) : Iterator<DataMap> {

        override fun next(): DataMap {
            return gsonDataMapDeserializer.fromJson(reader, DataMapF::class.java)
        }

        override fun hasNext(): Boolean {
            return reader.hasNext()
        }
    }

    val reader = JsonReader(InputStreamReader(ins, "UTF-8"))
    reader.beginArray()
    val iterator = StreamIterator(reader)
    return StreamSupport
            .stream(Spliterators.spliteratorUnknownSize<DataMap>(iterator,
                    CONCURRENT or IMMUTABLE), true)
            .onClose {
                try {
                    reader.endArray()
                    reader.close()
                } catch (e: IOException) {
                    throw RuntimeException(e)
                }
            }
}

private open class DMSerializer(val options: JsonWriteOptions) : JsonSerializer<DataMap> {

    private val cache = mutableMapOf<DataMap, DataMap>()

    override fun serialize(obj: DataMap, foo: Type, context: JsonSerializationContext): JsonElement {

        if (!obj.isDynamic() && options.writeTableEntityAsHeaders)
            return writeDmHeader(obj)

        cache[obj] = obj

        val jsonObject = JsonObject()

        if (options.writeEntityName()) {
            jsonObject.addProperty("entity", obj.entity)
        }
        if (options.writeSystemProps && !obj.isDynamic()) {
            addJsonPropertyForValue("id", obj.id, jsonObject)
        }

        obj.map.filter { options.writeSystemProps || !it.key.startsWith("__") }.forEach { t, u ->
            addJsonNode(obj, u, t, jsonObject, context)
        }
        return jsonObject
    }

    private fun writeDmHeader(obj: DataMap): JsonElement {

        val jsonObject = JsonObject()

        jsonObject.addProperty("entity", obj.entity)
        addJsonPropertyForValue("id", obj.id, jsonObject)

        val captionField = obj.firstExistingField(*DataMap.captionFields)
        captionField?.let {
            addJsonPropertyForValue(captionField, obj[captionField], jsonObject)
        }
        return jsonObject
    }

    private fun addJsonPropertyForValue(prop: String, value: Any?, jsonObject: JsonObject) {
        when (value) {
            null -> jsonObject.addProperty(prop, value?.toString())
            is Number -> jsonObject.addProperty(prop, value)
            is String -> jsonObject.addProperty(prop, value)
            is Boolean -> jsonObject.addProperty(prop, value)
            else -> jsonObject.addProperty(prop, value.toString())
        }
    }

    fun addJsonNode(obj: DataMap, u: Any?, t: String, jsonObject: JsonObject, context: JsonSerializationContext) {

        val flag = !options.writeJsonTechProps &&
                (t.equals(DataMap.JSON_PATH, true)
                        || t.equals(DataMap.DYNAMIC_HOLDER, true)
                        || (t.equals(DataMap.ID, true) && obj.isDynamic()))
        if (flag)
            return

        when (u) {
            is DataMap -> {

                //todo: клауз !u.isDynamic() приведет к зацикливанию если в паре динамических объектах есть ссылки друг на друга
                if (!u.isDynamic() && (cache.containsKey(u) || t == DYNAMIC_HOLDER)) {
                    if (!options.notWriteBackRefs()) {
                        val map = DataMapF<UndefinedFieldSet>(u.entity, u.id, props = mapOf("isBackRef" to true))

                        u.getNativeKeyValue()?.let {
                            map[it.first, true] = it.second
                        }
                        jsonObject.add(t, context.serialize(map))
                    }
                } else {
                    jsonObject.add(t, context.serialize(u))
                }
            }
            is Collection<*> -> {
                val arr = JsonArray()
                u.forEach { e -> arr.add(context.serialize(e)) }
                jsonObject.add(t, arr)
            }
            is java.util.Date ->
                jsonObject.addProperty(t, formatDate(u))
            is Date ->
                jsonObject.addProperty(t, formatDate(u.date))


            else -> addJsonPropertyForValue(t, u, jsonObject)
        }
    }
}

private class DmJsonDeserializer : DmJsonDeserializerF<UndefinedFieldSet>(null)

private open class DmJsonDeserializerF<T : MappingFieldSet<T>>(val fieldSet: MappingFieldSet<T>? = null) : JsonDeserializer<DataMapF<T>> {


    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): DataMapF<T> {
        val jobject = json.asJsonObject
        return getDmFromJsonObject(jobject, fieldSet as? MFS<*>) as DataMapF<T>
    }

    fun getDmFromJsonObject(jObject: JsonObject, fieldSet: MappingFieldSet<*>?): DataMap {

        //создаем карту
        val entity = if (jObject.get("entity") == null) DataMap.DYNAMIC_ENTITY else jObject.get("entity").asString

        val id = extractValue(jObject.get("id"), "id", null)

        val dm = createDataMap(entity, id, fieldSet)

        //добавляем таг:
        jObject.keySet()
                .filter {
                    !(it.equals("isBackRef", true) ||
                            it.equals("entity", true))
                }
                .forEach {
                    val propValue = jObject[it]
                    dm[it, true] = extractValue(propValue, it, fieldSet)
                }
        return dm
    }

    private fun createDataMap(entity: String, id: Any?, fieldSet: MappingFieldSet<*>?): DataMap {
        if (fieldSet != null)
            return fieldSet.existing(id) {}

        val fsByEntity = FieldSetRepo.fieldSetOrNull(entity)

        return if (fsByEntity != null)
            return (fsByEntity as MFS<*>).existing(id) {}
        else
            DataMapF<UndefinedFieldSet>(entity, id, isNew = false)
    }

    private fun extractValue(propValue: JsonElement?, parentProperty: String?, parentFieldSet: MappingFieldSet<*>?): Any? {
        return when (propValue) {
            null -> null
            is JsonNull -> null
            is JsonPrimitive -> getValueFromJsonPrimitive(null, parentProperty, propValue)
            is JsonObject -> {
                val childFiledSet = parentFieldSet?.getReferencedFieldSet(parentProperty!!)
                getDmFromJsonObject(propValue, childFiledSet)
            }
            is JsonArray -> {
                val list = mutableListOf<Any?>()
                propValue.forEach { p ->
                    list.add(extractValue(p, parentProperty, parentFieldSet))
                }
                list
            }
            else -> TODO()
        }
    }
}

@Service
@Primary
internal open class JsonFieldDataMapsBuilder {
    open fun buildDataMapFromJson(holder: DataMap, property: String, json: String?, projection: DataProjection? = null): DataMap? {
        return dbDataMapFromJson(holder, property, json, projection)
    }
}

fun dbDataMapFromJson(holder: DataMap, property: String, json: String?,
                      projection: DataProjection? = null): DataMap? {
    if (json == null)
        return null

    val gson = GsonBuilder()
            .registerTypeAdapter(DataMapF::class.java, DmDbJsonDeserializer(holder, property, projection))
            .create()
    return gson.fromJson(json, DataMapF::class.java)
}

/***
 * Десериалазйер для работы с json-колонками базы
 */
internal open class DmDbJsonDeserializer(val holder: DataMap,
                                         val property: String,
                                         val projection: DataProjection? = null) : JsonDeserializer<DataMap> {


    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): DataMap {
        val jobject = json.asJsonObject
        val parentFieldSet = holder.fieldSet
        return getDmFromJsonObject(jobject, parentIdPath = "",
                parentProperty = property,
                fullParentProperty = property,
                parentFieldSet = parentFieldSet,
                parentProjection = projection)
    }

    private fun getDmFromJsonObject(jobject: JsonObject, parentIdPath: String,
                                    parentProperty: String,
                                    fullParentProperty: String,
                                    parentFieldSet: FieldSet? = null,
                                    parentProjection: DataProjection? = null): DataMap {

        val jsonPath: Any? = buildJsonPath(parentIdPath)

        //создаем карту
        val pair = createDataMap(parentProperty, parentFieldSet, jobject)
        val dm = pair.first
        dm[DYNAMIC_HOLDER, true] = holder
        dm[JSON_PATH, true] = jsonPath

        jobject.keySet()
                .filter {
                    parentProjection == null ||
                            parentProjection.fields.isEmpty() ||
                            parentProjection.fields.containsKey(it) ||
                            it.equals(DataMap.ID, true)
                }
                .forEach {
                    val propValue = jobject[it]
                    val currPath = if (parentIdPath.isBlank()) it else "$parentIdPath.$it"
                    val currFullParentProperty = "$fullParentProperty.$it"
                    dm[it, true] = extractValue(propValue, currPath, it, currFullParentProperty, pair.second,
                            parentProjection?.fields?.get(it))
                }
        if (dm.id == null)
            dm.id = newSyntheticId()
        return dm
    }

    private fun createDataMap(parentProperty: String,
                              parentFieldSet: FieldSet?,
                              jobject: JsonObject): Pair<DataMap, FieldSet?> {

        //посмотрим не можем и даж не должны ли мы создать датамап
        //по имени entity
        if (jobject.has(DataMap.ENTITY)) {
            val entity = (jobject[DataMap.ENTITY] as? JsonPrimitive)?.asString
            entity?.let {
                val fs = FieldSetRepo.fieldSetOrNull(it)
                if (fs != null && !fs.isDynamic())
                    return Pair(DataMapF(fs, id = null, isNew = false), null)
            }
        }

        //в противном случае - ищем поле в родительском фиелдсете
        //и определяем по нему тип фиелдсета для создания датамапа
        if (parentFieldSet != null) {
            val field = parentFieldSet.getField(parentProperty)
            if (field != null) {
                val newFieldSet = field.refFieldSet() as? MFS
                if (newFieldSet != null && newFieldSet != UndefinedFieldSet) {
                    return Pair(newFieldSet.existing(null) {}, newFieldSet)
                }
            }
        }
        return Pair(DataMapF(DynamicFieldSet, id = null, isNew = false), null)
    }

    protected open fun extractValue(propValue: JsonElement?, currPath: String, parentProperty: String,
                                    fullParentProperty: String, parentFieldSet: FieldSet?,
                                    parentProjection: DataProjection? = null): Any? {
        return when (propValue) {
            null -> null
            is JsonNull -> null
            is JsonPrimitive -> getValueFromJsonPrimitive(parentFieldSet, parentProperty, propValue)
            is JsonObject -> getDmFromJsonObject(propValue, currPath, parentProperty, fullParentProperty,
                    parentFieldSet, parentProjection)
            is JsonArray -> {
                var i = 0
                val list = mutableListOf<Any?>()
                propValue.forEach { p ->
                    val currPathArr = "$currPath[$i]"
                    i++
                    list.add(extractValue(p, currPathArr, parentProperty, fullParentProperty,
                            parentFieldSet, parentProjection))
                }
                return if (list.size > 0 && list[0] !is DataMap)
                    PrimeList(list) else
                    list
            }
            else -> TODO()
        }
    }

    private fun buildJsonPath(parentPath: String): Any? {
        return "${holder.entity}:${holder.id}:$property" + (if (parentPath.isBlank()) "" else ":$parentPath")
    }
}

private fun getValueFromJsonPrimitive(parentFieldSet: FieldSet?, parentProperty: String?, propValue: JsonPrimitive): Any {
    return when {
        propValue.isBoolean -> propValue.asBoolean

        propValue.isNumber -> {
            when {
                propValue.asString.contains('.') -> propValue.asDouble
                Int.MAX_VALUE.toString().length > propValue.asString.length -> propValue.asInt
                else -> propValue.asLong
            }
        }
        propValue.isString -> {
            if (parentFieldSet != null && parentProperty != null) {
                val v = propValue.asString
                val f = parentFieldSet.getField(parentProperty)
                if (f != null && f.isEnum()) {
                    return f.parse(v)!!
                }
            }

            return parseDate(propValue.asString)

        }
        else -> TODO()
    }
}

private open class DmListJsonDeserializer : JsonDeserializer<List<DataMap>> {

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): List<DataMap> {
        val jobject = json.asJsonArray
        return getDmFromJsonObject(jobject)
    }

    fun getDmFromJsonObject(jObject: JsonArray): List<DataMap> {
        val list = ArrayList<DataMap>(jObject.size())
        jObject.forEach {
            val fromJson = dmJsonDeserializer.getDmFromJsonObject(it as JsonObject, null)
            list.add(fromJson)
        }
        return list
    }
}

private class DMListSerializer(val options: JsonWriteOptions = JsonWriteOptions()) : JsonSerializer<List<DataMap>> {

    override fun serialize(obj: List<DataMap>, foo: Type, context: JsonSerializationContext): JsonElement {
        val arr = JsonArray()
        val gson = GsonBuilder()
                .registerTypeAdapter(DataMapF::class.java, DMSerializer(options))
                .setPrettyPrinting()
                .serializeNulls()
                .create()
        obj.forEach { dm ->
            arr.add(gson.toJsonTree(dm))
        }
        return arr
    }
}


private val dmJsonDeserializer = DmJsonDeserializer()
private val gsonDataMapDeserializer = GsonBuilder()
        .registerTypeAdapter(DataMapF::class.java, dmJsonDeserializer)
        .create()

private val dateTimeFormatter = DateTimeFormatter.ofPattern(COMMON_DATE_FORMAT)
private val defaultZoneId = ZoneId.systemDefault()

fun parseDate(input: String): Any {
    if (input.length == 19 && input[4] == '-' && input[10] == 'T')
        return try {
            Timestamp(java.util.Date.from(LocalDateTime.parse(input, dateTimeFormatter)
                    .atZone(defaultZoneId)
                    .toInstant()))
        } catch (e: DateTimeParseException) {
            return input
        }
    return input
}

fun formatDate(date: java.util.Date): String {
    return dateTimeFormatter.format(date.toInstant()
            .atZone(defaultZoneId)
            .toLocalDateTime())
}

/*  *синтетический ключ применяется в динамик-объектах.
     * Отличие от гуида в том мы быстро можем отличить среди остальных строк и чисел*/
object SynthethicIdUtils {

    /**сгенерировать новый синтетический ключ.
     * синтетический ключ применяется в динамик-объектах.
     * Отличие от гуида в том мы быстро можем отличить среди остальных строк и чисел*/
    fun newSyntheticId(): String = "@ice-" + DeltaStore.newGuid()

    /**Это синтетический ключ? */
    fun isSyntheticId(id: String?): Boolean = id != null && id.startsWith("@ice-")

    /**Если синтетический ключ - вернуть null. Иначе - ключ*/
    fun getNoSyntheticIdOrNull(id: Any?): Any? {
        if (isSyntheticId(id as? String))
            return null
        return id
    }
}