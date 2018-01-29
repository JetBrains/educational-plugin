package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.INDEX
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.SUBTASK_INFOS
import java.lang.reflect.Type

class StepikSubmissionAnswerPlaceholderAdapter : JsonSerializer<AnswerPlaceholder> {
  override fun serialize(placeholder: AnswerPlaceholder, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
    val gson = GsonBuilder().setPrettyPrinting().excludeFieldsWithoutExposeAnnotation().create()
    val answerPlaceholderJson = gson.toJsonTree(placeholder)
    val answerPlaceholderObject = answerPlaceholderJson.asJsonObject
    val subtaskInfos = answerPlaceholderObject.getAsJsonObject(SUBTASK_INFOS)
    val infosArray = JsonArray()
    for ((key, value) in subtaskInfos.entrySet()) {
      val subtaskInfo = value.asJsonObject
      subtaskInfo.add(INDEX, JsonPrimitive(Integer.valueOf(key)))
      infosArray.add(subtaskInfo)
    }
    answerPlaceholderObject.remove(SUBTASK_INFOS)
    answerPlaceholderObject.add(SUBTASK_INFOS, infosArray)
    return answerPlaceholderJson
  }
}