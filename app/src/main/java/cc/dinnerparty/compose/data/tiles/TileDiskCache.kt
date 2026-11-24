package cc.dinnerparty.compose.data.tiles

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class TileDiskCache(private val directory: File) {
    suspend fun read(key: String): JsonObject? = withContext(Dispatchers.IO) {
        val file = File(directory, "$key.json")
        if (!file.exists()) return@withContext null
        check(file.setLastModified(System.currentTimeMillis()))
        JsonParser.parseString(file.readText()).asJsonObject
    }

    suspend fun write(key: String, body: JsonObject) = withContext(Dispatchers.IO) {
        check(directory.isDirectory || directory.mkdirs())
        val file = File(directory, "$key.json")
        file.writeText(body.toString())
        val files = checkNotNull(directory.listFiles()).sortedBy { it.lastModified() }
        var bytes = files.sumOf { it.length() }
        for (cached in files) {
            if (bytes <= 64L * 1024 * 1024) break
            bytes -= cached.length()
            check(cached.delete())
        }
    }
}
