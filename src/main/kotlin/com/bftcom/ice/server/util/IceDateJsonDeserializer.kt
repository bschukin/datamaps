package com.bftcom.ice.server.util

import com.bftcom.ice.common.utils.Date
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import org.springframework.boot.jackson.JsonComponent
import java.io.IOException

@JsonComponent
class IceDateJsonDeserializer : JsonDeserializer<Date>() {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Date {
        val milliseconds = jsonParser.codec.readValue(jsonParser, Long::class.java)
        return Date(milliseconds)
    }
}
