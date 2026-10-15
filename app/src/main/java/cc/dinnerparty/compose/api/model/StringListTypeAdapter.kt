package cc.dinnerparty.compose.api.model

import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

/**
 * `Place.exterior_images` / `menu_images` 的专用解析器。
 *
 * 后端把图片列表存在 TEXT 列里，值本身是 JSON 数组字符串，例如 `["https://.../a.jpg"]`；
 * 库里还混着历史脏数据（半截 JSON、`garbage` 之类，见后端 `tests/media/test_image_storage.js`）。
 *
 * 所以这里把「JSON 字符串」「JSON 数组」「坏值」三种情况都吃掉，坏值一律当空列表，
 * 不让一条脏数据把整个地点列表解析崩掉。
 */
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
