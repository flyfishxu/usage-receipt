package com.flyfishxu.usage.usage

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

val UsageJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    prettyPrint = true
}

fun JsonElement?.obj(): JsonObject? = this as? JsonObject

fun JsonElement?.array(): JsonArray? = this as? JsonArray

fun JsonElement?.stringOrNull(): String? = (this as? JsonPrimitive)?.contentOrNull

fun JsonElement?.longOrZero(): Long = (this as? JsonPrimitive)?.longOrNull ?: 0L

fun JsonElement?.booleanOrFalse(): Boolean = (this as? JsonPrimitive)?.booleanOrNull ?: false

fun JsonObject.string(name: String): String? = this[name].stringOrNull()

fun JsonObject.long(name: String): Long = this[name].longOrZero()

fun JsonObject.obj(name: String): JsonObject? = this[name].obj()

fun JsonObject.array(name: String): JsonArray? = this[name].array()

fun JsonObject.boolean(name: String): Boolean = this[name].booleanOrFalse()

fun JsonObject.nonNull(name: String): JsonElement? = this[name]?.takeUnless { it is JsonNull }

fun parseJsonObjectOrNull(raw: String): JsonObject? =
    runCatching { UsageJson.parseToJsonElement(raw).obj() }.getOrNull()
