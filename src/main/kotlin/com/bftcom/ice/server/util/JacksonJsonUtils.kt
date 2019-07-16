package com.bftcom.ice.server.util

import com.bftcom.ice.common.maps.DataMap
import com.bftcom.ice.common.maps.DataMapF
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.core.io.Resource
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport

val JACKSON = ObjectMapper().apply {
    val module = SimpleModule()
    module.addDeserializer(DataMapF::class.java, DatamapJacksonDeserializer())
    this.registerModule(module)
}.registerKotlinModule()

@Deprecated("Use JACKSON.readValue() instead")
fun jsonToMap(json: String): DataMap {
    return JACKSON.readValue(json)
}

@Deprecated("Use JACKSON.readValue() instead")
fun jsonToMaps(json: String?): List<DataMap> {
    if (json == null)
        return emptyList()

    return JACKSON.readValue(json)
}

@Deprecated("Use JACKSON.readValue() instead")
fun streamJsonToMaps(ins: InputStream): List<DataMap> {
    return JACKSON.readValue(ins)
}

fun Resource.toDataMapStream(): Stream<DataMap> {
    return if (isFile) {
        file.toDataMapStream()
    } else if (inputStream.markSupported()) {
        val cs = Charset.forName( "UTF-8")
        val res: List<DataMap> = JACKSON.readValue(inputStream.reader(cs))
        res.stream()
    } else {
        throw RuntimeException("Invalid data provided")
    }
}

fun File.toDataMapStream(): Stream<DataMap> {
    class StreamIterator(val parser: JsonParser) : Iterator<DataMap> {
        var current: JsonToken? = null
        //val dseri = DatamapJacksonDeserializer()

        override fun next(): DataMap {
            // current = parser.nextToken()
            //val node: JsonNode = parser.readValueAsTree()
            return JACKSON.readValue(parser)
            //return dseri.getDmFromJsonObject(node)
        }

        override fun hasNext(): Boolean {
            current = parser.nextToken()
            return current != null && current != JsonToken.END_ARRAY
        }

    }

    val f = MappingJsonFactory()
    val jp = f.createParser(this)
    val current: JsonToken
    current = jp.nextToken()
    if (current !== JsonToken.START_ARRAY) {
        TODO()
    }

    val iterator = StreamIterator(jp)
    return StreamSupport
            .stream(Spliterators.spliteratorUnknownSize<DataMap>(iterator,
                    Spliterator.CONCURRENT or Spliterator.IMMUTABLE), true).onClose {
                // println("==============>CLOSED")
            }
}

@Deprecated("Use .toDataMapStream() instead")
fun jsonToStream(ins: InputStream): Stream<DataMap> {

    class StreamIterator(val parser: JsonParser) : Iterator<DataMap> {
        var current: JsonToken? = null
        val dseri = DatamapJacksonDeserializer()

        override fun next(): DataMap {
            // current = parser.nextToken()
            val node: JsonNode = parser.readValueAsTree()
            return dseri.getDmFromJsonObject(node)
        }

        override fun hasNext(): Boolean {
            current = parser.nextToken()
            return current != null && current != JsonToken.END_ARRAY
        }

    }

    val f = MappingJsonFactory()
    val jp = f.createParser(ins)
    val current: JsonToken
    current = jp.nextToken()
    if (current !== JsonToken.START_ARRAY) {
        TODO()
    }

    val iterator = StreamIterator(jp)
    return StreamSupport
            .stream(Spliterators.spliteratorUnknownSize<DataMap>(iterator,
                    Spliterator.CONCURRENT or Spliterator.IMMUTABLE), true).onClose {
                // println("==============>CLOSED")
            }
}

private class DatamapJacksonDeserializer : JsonDeserializer<DataMap>() {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): DataMap {

        val oc = jp.getCodec()
        val node: JsonNode = oc.readTree(jp)

        return getDmFromJsonObject(node)
    }

    fun getDmFromJsonObject(node: JsonNode): DataMap {
        val entity = if (node.get("entity") == null) DataMap.DYNAMIC_ENTITY else node.get("entity").asText()
        val dm = DataMap(entity, if (node.get("id") != null) getValueFromJsonPrimitive(node.get("id")) else null, isNew = false)

        //.filter { it != DataMap.ID }.forEach {
        node.fields().forEach {
            //if (!DataMap.ID.equals(it.key, true))
            dm[it.key, true] = extractValue(it.value)
        }

        return dm
    }


    private fun extractValue(propValue: JsonNode?): Any? {
        return when {
            propValue == null -> null
            propValue.isNull -> null
            propValue.isNumber
                    || propValue.isTextual
                    || propValue.isBoolean -> getValueFromJsonPrimitive(propValue)
            propValue.isObject -> getDmFromJsonObject(propValue)
            propValue.isArray -> {
                val list = mutableListOf<Any?>()
                propValue.forEach { p ->
                    list.add(extractValue(p))
                }
                list
            }
            else -> TODO()
        }
    }

    private fun getValueFromJsonPrimitive(propValue: JsonNode): Any? {
        return when {
            propValue.isBoolean -> propValue.booleanValue()
            propValue.isNull -> null
            propValue.isNumber -> {
                if (propValue.isDouble)
                    propValue.doubleValue()
                else if (propValue.isLong)
                    propValue.longValue()
                else if (propValue.isInt)
                    propValue.intValue()
                else TODO()
            }
            propValue.isTextual -> {
                return parseDate(propValue.textValue())
            }
            else -> TODO()
        }
    }
}




