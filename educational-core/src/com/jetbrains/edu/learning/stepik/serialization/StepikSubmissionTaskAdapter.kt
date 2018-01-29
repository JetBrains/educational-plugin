package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderSubtaskInfo
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TaskWithSubtasks
import com.jetbrains.edu.learning.serialization.SerializationUtils
import java.lang.reflect.Type

class StepikSubmissionTaskAdapter : JsonSerializer<Task>, JsonDeserializer<Task> {

  override fun serialize(src: Task, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .registerTypeAdapter(AnswerPlaceholderSubtaskInfo::class.java,
                           StepikSubmissionSubtaskInfoAdapter())
      .create()
    val taskObject = SerializationUtils.Json.serializeWithTaskType(src, gson)

    if (src is TaskWithSubtasks) {
      taskObject.add("active_subtask_index", JsonPrimitive(src.activeSubtaskIndex))
    }

    return taskObject
  }

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Task? {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .registerTypeAdapter(AnswerPlaceholderSubtaskInfo::class.java,
                           StepikSubmissionSubtaskInfoAdapter())
      .create()

    return SerializationUtils.Json.doDeserialize(json, gson)
  }
}

internal class StepikSubmissionSubtaskInfoAdapter : JsonSerializer<AnswerPlaceholderSubtaskInfo>, JsonDeserializer<AnswerPlaceholderSubtaskInfo> {

  override fun serialize(src: AnswerPlaceholderSubtaskInfo, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement {
    val gson = GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create()
    val subTaskInfo = gson.toJsonTree(src).asJsonObject

    subTaskInfo.add("selected", JsonPrimitive(src.selected))
    subTaskInfo.add("status", JsonPrimitive(src.status.toString()))

    return subTaskInfo
  }

  @Throws(JsonParseException::class)
  override fun deserialize(jsonElement: JsonElement,
                           type: Type,
                           jsonDeserializationContext: JsonDeserializationContext): AnswerPlaceholderSubtaskInfo {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonObject = jsonElement.asJsonObject
    val subtaskInfo = gson.fromJson(jsonObject, AnswerPlaceholderSubtaskInfo::class.java)

    if (jsonObject.has("selected")) {
      subtaskInfo.selected = jsonObject.get("selected").asBoolean
    }

    if (jsonObject.has("status")) {
      subtaskInfo.status = CheckStatus.valueOf(jsonObject.get("status").asString)
    }

    return subtaskInfo
  }
}
