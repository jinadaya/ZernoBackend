package app.zerno.backend.content

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.security.MessageDigest

/**
 * Reading plans and achievement definitions the app can refresh without a release, read once from [dir]:
 * `plans/index.json` (plan ids in list order), `plans/<id>.json` and `achievements.json`, in the same formats the
 * app bundles. The server checks only what it needs to serve them; the app parses and validates the rest.
 *
 * Each response carries a content [version] (a hash of its documents), so it changes whenever a file does and
 * doubles as the `ETag`.
 */
class ContentCatalog(dir: File) {
    val plans: Document
    val achievements: Document

    init {
        require(dir.isDirectory) { "No content directory at ${dir.absolutePath}" }
        val ids = read(File(dir, "plans/index.json")).jsonArray.map { it.jsonPrimitive.content }
        require(ids.isNotEmpty()) { "plans/index.json lists no plans" }
        require(ids.toSet().size == ids.size) { "plans/index.json lists a plan twice" }
        val planList = ids.map { id -> read(File(dir, "plans/$id.json")).jsonObject.also { checkPlan(id, it) } }
        plans = Document.of("plans", JsonArray(planList))

        val catalog = read(File(dir, "achievements.json")).jsonObject
        val list = catalog.getValue("achievements").jsonArray
        val codes = list.map { it.jsonObject.getValue("code").jsonPrimitive.content }
        require(codes.toSet().size == codes.size) { "achievements.json has a duplicate code" }
        achievements = Document.of(
            "achievements",
            list,
            schemaVersion = catalog.getValue("version").jsonPrimitive.intOrNull
                ?: error("achievements.json needs a numeric version"),
        )
    }

    /** One response body: [body] is what the route sends, [version] its content hash. */
    class Document private constructor(val version: String, val body: JsonObject) {
        companion object {
            fun of(key: String, items: JsonArray, schemaVersion: Int? = null): Document {
                val version = hash(Json.encodeToString(JsonElement.serializer(), items))
                val body = buildJsonObject {
                    put("version", version)
                    if (schemaVersion != null) put("schema_version", schemaVersion)
                    put(key, items)
                }
                return Document(version, body)
            }
        }
    }

    private companion object {
        const val VERSION_LENGTH = 16

        fun read(file: File): JsonElement {
            require(file.isFile) { "Missing content file ${file.absolutePath}" }
            return Json.parseToJsonElement(file.readText())
        }

        fun checkPlan(id: String, plan: JsonObject) {
            require(plan["id"]?.jsonPrimitive?.content == id) { "plans/$id.json has a different id" }
            require(plan["days"]?.jsonArray?.isNotEmpty() == true) { "plans/$id.json has no days" }
        }

        fun hash(text: String): String = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(VERSION_LENGTH)
    }
}
