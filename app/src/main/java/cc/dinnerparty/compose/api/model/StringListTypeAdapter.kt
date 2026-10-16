package cc.dinnerparty.compose.api.model

import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

internal class StringListTypeAdapter : TypeAdapter<List<String>?>() {

    override fun write(out: JsonWriter, value: List<String>?) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginArray()
        value.forEach(out::value)
        out.endArray()
    }

    override fun read(reader: JsonReader): List<String>? = when (reader.peek()) {
        JsonToken.NULL -> {
            reader.nextNull()
            null
        }

        JsonToken.STRING -> parseArrayString(reader.nextString())

        JsonToken.BEGIN_ARRAY -> buildList {
            reader.beginArray()
            while (reader.hasNext()) {
                if (reader.peek() == JsonToken.STRING) add(reader.nextString()) else reader.skipValue()
            }
            reader.endArray()
        }

        else -> {
            reader.skipValue()
            null
        }
    }

    private fun parseArrayString(raw: String): List<String> {
        val text = raw.trim()
        if (text.isEmpty()) return emptyList()
        return runCatching {
            JsonParser.parseString(text).asJsonArray.mapNotNull { element ->
                element.takeIf { it.isJsonPrimitive }?.asString
            }
        }.getOrDefault(emptyList())
    }
}
