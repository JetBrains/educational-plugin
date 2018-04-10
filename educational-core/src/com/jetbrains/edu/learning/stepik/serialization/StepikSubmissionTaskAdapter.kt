package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.stepik.StepikWrappers
import java.lang.reflect.Type

class StepikReplyAdapter : JsonDeserializer<StepikWrappers.Reply> {

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): StepikWrappers.Reply {
    val jsonObject = json.asJsonObject
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.fromJson<StepikWrappers.Reply>(jsonObject).apply {
      version = jsonObject.getAsJsonPrimitive("version")?.asInt ?: 1
    }
  }
}

class StepikSubmissionTaskAdapter(replyVersion: Int = JSON_FORMAT_VERSION) : JsonSerializer<Task>, JsonDeserializer<Task> {

  private val placeholderAdapter = StepikSubmissionAnswerPlaceholderAdapter(replyVersion)

  override fun serialize(src: Task, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .registerTypeAdapter(AnswerPlaceholder::class.java, placeholderAdapter)
      .create()
    return SerializationUtils.Json.serializeWithTaskType(src, gson)
  }

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Task? {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .registerTypeAdapter(AnswerPlaceholder::class.java, placeholderAdapter)
      .create()

    return SerializationUtils.Json.doDeserialize(json, gson)
  }
}

private class StepikSubmissionAnswerPlaceholderAdapter(private val replyVersion: Int) : JsonSerializer<AnswerPlaceholder>, JsonDeserializer<AnswerPlaceholder> {

  override fun serialize(src: AnswerPlaceholder, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
    val gson = GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create()
    val placeholderObject = gson.toJsonTree(src).asJsonObject

    placeholderObject.add("selected", JsonPrimitive(src.selected))
    placeholderObject.add("status", JsonPrimitive(src.status.toString()))

    return placeholderObject
  }

  @Throws(JsonParseException::class)
  override fun deserialize(jsonElement: JsonElement,
                           type: Type,
                           jsonDeserializationContext: JsonDeserializationContext): AnswerPlaceholder {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val placeholderObject = jsonElement.asJsonObject.migrate(replyVersion)
    val placeholder = gson.fromJson<AnswerPlaceholder>(placeholderObject)

    if (placeholderObject.has("selected")) {
      placeholder.selected = placeholderObject.get("selected").asBoolean
    }

    if (placeholderObject.has("status")) {
      placeholder.status = CheckStatus.valueOf(placeholderObject.get("status").asString)
    }

    return placeholder
  }

  private fun JsonObject.migrate(version: Int): JsonObject {
    var jsonObject = this
    @Suppress("NAME_SHADOWING")
    var version = version
    while (version < JSON_FORMAT_VERSION) {
      when (version) {
        1 -> jsonObject = SerializationUtils.Json.removeSubtaskInfo(jsonObject)
      }
      version++
    }

    return jsonObject
  }
}

private inline fun <reified T> Gson.fromJson(element: JsonElement): T = fromJson(element, T::class.java)
