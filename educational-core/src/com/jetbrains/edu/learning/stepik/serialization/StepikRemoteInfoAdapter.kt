package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.remote.StepikRemoteInfo
import com.jetbrains.edu.learning.stepik.StepikWrappers
import java.lang.reflect.Type

class StepikRemoteInfoAdapter(val language: String?) : JsonDeserializer<Course>, JsonSerializer<Course> {
  private val IS_PUBLIC = "is_public"

  override fun serialize(course: Course?, type: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .create()
    val tree = gson.toJsonTree(course)
    val jsonObject = tree.asJsonObject
    val remoteInfo = course?.remoteInfo
    jsonObject.add(IS_PUBLIC, JsonPrimitive((remoteInfo as? StepikRemoteInfo)?.isPublic ?: false))
    return jsonObject
  }

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext): Course {
    val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(StepikWrappers.StepOptions::class.java, StepikStepOptionsAdapter(language))
      .registerTypeAdapter(Lesson::class.java, StepikLessonAdapter(language))
      .registerTypeAdapter(StepikWrappers.Reply::class.java, StepikReplyAdapter(language))
      .create()

    val course = gson.fromJson(json, RemoteCourse::class.java)
    deserializeRemoteInfo(json, course)
    return course
  }

  private fun deserializeRemoteInfo(json: JsonElement, course: RemoteCourse) {
    val jsonObject = json.asJsonObject
    val remoteInfo = StepikRemoteInfo()
    val isPublic = jsonObject.get(IS_PUBLIC).asBoolean
    remoteInfo.isPublic = isPublic
    course.remoteInfo = remoteInfo
  }
}
