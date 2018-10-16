package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.remote.LocalInfo
import com.jetbrains.edu.learning.courseFormat.remote.RemoteInfo
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikUtils
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikCourseRemoteInfo
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikLessonRemoteInfo
import com.jetbrains.edu.learning.stepik.courseFormat.remoteInfo.StepikSectionRemoteInfo
import org.fest.util.Lists
import java.lang.reflect.Type
import java.util.*

private const val UPDATE_DATE = "update_date"
private const val ID = "id"
private const val IS_PUBLIC = "is_public"
private const val STEPS = "steps"
private const val COURSE_ID = "course"
private const val POSITION = "position"
private const val UNITS = "units"
private const val IS_IDEA_COMPATIBLE = "is_idea_compatible"
private const val SECTIONS = "sections"
private const val INSTRUCTORS = "instructors"
private const val COURSE_FORMAT = "course_format"

// These adapters should be used everywhere we communicate with stepik
object StepikCourseRemoteInfoAdapter : JsonDeserializer<StepikCourse>, JsonSerializer<Course> {

  override fun serialize(course: Course?, type: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = getGson()
    val tree = gson.toJsonTree(course)
    val jsonObject = tree.asJsonObject
    val remoteInfo = course?.remoteInfo

    val stepikRemoteInfo = remoteInfo as? StepikCourseRemoteInfo

    jsonObject.add(IS_PUBLIC, JsonPrimitive(stepikRemoteInfo?.isPublic ?: false))
    jsonObject.add(IS_IDEA_COMPATIBLE, JsonPrimitive(stepikRemoteInfo?.isIdeaCompatible ?: false))
    jsonObject.add(ID, JsonPrimitive(stepikRemoteInfo?.id ?: 0))
    jsonObject.add(SECTIONS, gson.toJsonTree(stepikRemoteInfo?.sectionIds ?: Lists.emptyList<Int>()))
    jsonObject.add(INSTRUCTORS, gson.toJsonTree(stepikRemoteInfo?.instructors ?: Lists.emptyList<Int>()))
    jsonObject.add(COURSE_FORMAT, JsonPrimitive(stepikRemoteInfo?.courseFormat ?: ""))

    val updateDate = stepikRemoteInfo?.updateDate
    if (updateDate != null) {
      val date = gson.toJsonTree(updateDate)
      jsonObject.add(UPDATE_DATE, date)
    }
    return jsonObject
  }

  @Throws(JsonParseException::class)
  override fun deserialize(json: JsonElement, type: Type, jsonDeserializationContext: JsonDeserializationContext): StepikCourse {
    val gson = getGson()

    val course = gson.fromJson(json, StepikCourse::class.java)
    course.remoteInfo = deserializeCourseRemoteInfo(json, gson)
    course.updateCourseCompatibility()
    return course
  }

  private fun getGson(): Gson {
    return GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(Lesson::class.java, StepikLessonRemoteInfoAdapter)
      .registerTypeAdapter(Section::class.java, StepikSectionRemoteInfoAdapter)
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
  }
}

object StepikSectionRemoteInfoAdapter : JsonDeserializer<Section>, JsonSerializer<Section> {
  override fun serialize(section: Section?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = getGson()
    val tree = gson.toJsonTree(section)
    val jsonObject = tree.asJsonObject
    val remoteInfo = section?.remoteInfo

    val stepikRemoteInfo = remoteInfo as? StepikSectionRemoteInfo

    jsonObject.add(ID, JsonPrimitive(stepikRemoteInfo?.id ?: 0))
    jsonObject.add(COURSE_ID, JsonPrimitive(stepikRemoteInfo?.courseId ?: 0))
    jsonObject.add(POSITION, JsonPrimitive(stepikRemoteInfo?.position ?: 0))
    jsonObject.add(UNITS, gson.toJsonTree(stepikRemoteInfo?.units ?: Lists.emptyList<Int>()))

    val updateDate = stepikRemoteInfo?.updateDate
    if (updateDate != null) {
      val date = gson.toJsonTree(updateDate)
      jsonObject.add(UPDATE_DATE, date)
    }
    return jsonObject
  }

  override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Section {
    val gson = getGson()

    val section = gson.fromJson(json, Section::class.java)
    section.remoteInfo = deserializeSectionRemoteInfo(gson, json)
    return section
  }

  private fun getGson(): Gson {
    return GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(Lesson::class.java, StepikLessonRemoteInfoAdapter)
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
  }
}

object StepikLessonRemoteInfoAdapter : JsonDeserializer<Lesson>, JsonSerializer<Lesson> {

  override fun serialize(lesson: Lesson?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = getGson()

    val tree = gson.toJsonTree(lesson)
    val jsonObject = tree.asJsonObject
    val remoteInfo = lesson?.remoteInfo

    val stepikRemoteInfo = remoteInfo as? StepikLessonRemoteInfo

    jsonObject.add(ID, JsonPrimitive(stepikRemoteInfo?.id ?: 0))
    jsonObject.add(IS_PUBLIC, JsonPrimitive(stepikRemoteInfo?.isPublic ?: false))
    jsonObject.add(STEPS, gson.toJsonTree(stepikRemoteInfo?.steps ?: Lists.emptyList<Int>()))

    val updateDate = stepikRemoteInfo?.updateDate
    if (updateDate != null) {
      val date = gson.toJsonTree(updateDate)
      jsonObject.add(UPDATE_DATE, date)
    }

    return jsonObject
  }

  override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Lesson {
    val gson = getGson()

    val lesson = gson.fromJson(json, Lesson::class.java)
    lesson.remoteInfo = deserializeLessonRemoteInfo(gson, json)
    renameAdditionalInfo(lesson)
    return lesson
  }

  private fun renameAdditionalInfo(lesson: Lesson) {
    val name = lesson.name
    if (StepikNames.PYCHARM_ADDITIONAL == name) {
      lesson.name = EduNames.ADDITIONAL_MATERIALS
    }
  }

  private fun getGson(): Gson {
    return GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
  }
}

// Some local zip archives contain stepik remote info (id and update_date)
// these functions are used both for stepik communication and deserializing local courses

fun deserializeCourseRemoteInfo(json: JsonElement, gson: Gson): RemoteInfo {
  val jsonObject = json.asJsonObject
  if (!jsonObject.has(UPDATE_DATE)) return LocalInfo()

  val remoteInfo = StepikCourseRemoteInfo()
  val isPublic = jsonObject.get(IS_PUBLIC)?.asBoolean ?: false
  val isCompatible = jsonObject.get(IS_IDEA_COMPATIBLE)?.asBoolean ?: false
  val id = jsonObject.get(ID).asInt
  val courseFormat = jsonObject.get(COURSE_FORMAT)?.asString ?: StepikUtils.getCourseFormat("")

  val sections = gson.fromJson<MutableList<Int>>(jsonObject.get(SECTIONS), object: TypeToken<MutableList<Int>>(){}.type) ?: mutableListOf()
  val instructors = gson.fromJson<MutableList<Int>>(jsonObject.get(INSTRUCTORS), object: TypeToken<MutableList<Int>>(){}.type) ?: mutableListOf()
  val updateDate = gson.fromJson(jsonObject.get(UPDATE_DATE), Date::class.java)

  remoteInfo.isPublic = isPublic
  remoteInfo.isIdeaCompatible = isCompatible
  remoteInfo.id = id
  remoteInfo.sectionIds = sections
  remoteInfo.instructors = instructors
  remoteInfo.updateDate = updateDate
  remoteInfo.courseFormat = courseFormat

  return remoteInfo
}

fun deserializeSectionRemoteInfo(gson: Gson, json: JsonElement): RemoteInfo {
  val jsonObject = json.asJsonObject
  if (!jsonObject.has(UPDATE_DATE)) return LocalInfo()

  val remoteInfo = StepikSectionRemoteInfo()
  val id = jsonObject.get(ID).asInt
  val courseId = jsonObject.get(COURSE_ID)?.asInt ?: 0
  val position = jsonObject.get(POSITION)?.asInt ?: 0
  val updateDate = gson.fromJson(jsonObject.get(UPDATE_DATE), Date::class.java)
  val units = gson.fromJson<MutableList<Int>>(jsonObject.get(UNITS), object: TypeToken<MutableList<Int>>(){}.type) ?: mutableListOf()

  remoteInfo.id = id
  remoteInfo.courseId = courseId
  remoteInfo.position = position
  remoteInfo.updateDate = updateDate
  remoteInfo.units = units

  return remoteInfo
}

fun deserializeLessonRemoteInfo(gson: Gson, json: JsonElement) : RemoteInfo {
  val jsonObject = json.asJsonObject
  if (!jsonObject.has(UPDATE_DATE)) return LocalInfo()

  val remoteInfo = StepikLessonRemoteInfo()

  val id = jsonObject.get(ID).asInt
  val isPublic = jsonObject.get(IS_PUBLIC)?.asBoolean ?: false
  val updateDate = gson.fromJson(jsonObject.get(UPDATE_DATE), Date::class.java)
  val steps = gson.fromJson<MutableList<Int>>(jsonObject.get(STEPS), object: TypeToken<MutableList<Int>>(){}.type)

  remoteInfo.id = id
  remoteInfo.isPublic = isPublic
  remoteInfo.updateDate = updateDate
  remoteInfo.steps = steps

  return remoteInfo
}
