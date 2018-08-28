package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StepikCourse
import com.jetbrains.edu.learning.courseFormat.remote.StepikRemoteInfo
import com.jetbrains.edu.learning.stepik.StepikWrappers
import org.fest.util.Lists
import java.lang.reflect.Type
import java.util.*

class StepikRemoteInfoAdapter : JsonDeserializer<Course>, JsonSerializer<Course> {
  private val IS_PUBLIC = "is_public"
  private val IS_ADAPTIVE = "is_adaptive"
  private val IS_IDEA_COMPATIBLE = "is_idea_compatible"
  private val ID = "id"
  private val UPDATE_DATE = "update_date"
  private val SECTIONS = "sections"
  private val INSTRUCTORS = "instructors"

  override fun serialize(course: Course?, type: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
    val tree = gson.toJsonTree(course)
    val jsonObject = tree.asJsonObject
    val remoteInfo = course?.remoteInfo

    jsonObject.add(IS_PUBLIC, JsonPrimitive((remoteInfo as? StepikRemoteInfo)?.isPublic ?: false))
    jsonObject.add(IS_ADAPTIVE, JsonPrimitive((remoteInfo as? StepikRemoteInfo)?.isAdaptive ?: false))
    jsonObject.add(IS_IDEA_COMPATIBLE, JsonPrimitive((remoteInfo as? StepikRemoteInfo)?.isIdeaCompatible ?: false))
    jsonObject.add(ID, JsonPrimitive((remoteInfo as? StepikRemoteInfo)?.id ?: 0))
    jsonObject.add(SECTIONS, gson.toJsonTree((remoteInfo as? StepikRemoteInfo)?.sectionIds ?: Lists.emptyList<Int>()))
    jsonObject.add(INSTRUCTORS, gson.toJsonTree((remoteInfo as? StepikRemoteInfo)?.instructors ?: Lists.emptyList<Int>()))

    val updateDate = (remoteInfo as? StepikRemoteInfo)?.updateDate
    if (updateDate != null) {
      val date = gson.toJsonTree(updateDate)
      jsonObject.add(UPDATE_DATE, date)
    }
    return jsonObject
  }

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext): Course {
    val gson = GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(StepikWrappers.StepOptions::class.java, StepikStepOptionsAdapter())
      .registerTypeAdapter(Lesson::class.java, StepikLessonAdapter())
      .registerTypeAdapter(StepikWrappers.Reply::class.java, StepikReplyAdapter())
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()

    val course = gson.fromJson(json, StepikCourse::class.java)
    deserializeRemoteInfo(json, course, gson)
    return course
  }

  private fun deserializeRemoteInfo(json: JsonElement, course: StepikCourse, gson: Gson) {
    val jsonObject = json.asJsonObject
    val remoteInfo = StepikRemoteInfo()
    val isPublic = jsonObject.get(IS_PUBLIC).asBoolean
    val isAdaptive = jsonObject.get(IS_ADAPTIVE).asBoolean
    val isCompatible = jsonObject.get(IS_IDEA_COMPATIBLE).asBoolean
    val id = jsonObject.get(ID).asInt

    val sections = gson.fromJson<List<Int>>(jsonObject.get(SECTIONS), object: TypeToken<List<Int>>(){}.type)
    val instructors = gson.fromJson<List<Int>>(jsonObject.get(INSTRUCTORS), object: TypeToken<List<Int>>(){}.type)
    val updateDate = gson.fromJson(jsonObject.get(UPDATE_DATE), Date::class.java)

    remoteInfo.isPublic = isPublic
    remoteInfo.isAdaptive = isAdaptive
    remoteInfo.isIdeaCompatible = isCompatible
    remoteInfo.id = id
    remoteInfo.sectionIds = sections
    remoteInfo.instructors = instructors
    remoteInfo.updateDate = updateDate

    course.remoteInfo = remoteInfo
  }
}
