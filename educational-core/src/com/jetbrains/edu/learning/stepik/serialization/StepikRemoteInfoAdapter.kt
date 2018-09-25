package com.jetbrains.edu.learning.stepik.serialization

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.courseFormat.*
import org.fest.util.Lists
import java.lang.reflect.Type
import java.util.*

class StepikCourseRemoteInfoAdapter(val language: String?) : JsonDeserializer<StepikCourse>, JsonSerializer<StepikCourse> {
  private val IS_PUBLIC = "is_public"
  private val IS_ADAPTIVE = "is_adaptive"
  private val IS_IDEA_COMPATIBLE = "is_idea_compatible"
  private val ID = "id"
  private val UPDATE_DATE = "update_date"
  private val SECTIONS = "sections"
  private val INSTRUCTORS = "instructors"

  override fun serialize(course: StepikCourse?, type: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = getGson()
    val tree = gson.toJsonTree(course)
    val jsonObject = tree.asJsonObject
    val remoteInfo = course?.remoteInfo

    val stepikRemoteInfo = remoteInfo as? StepikCourseRemoteInfo

    jsonObject.add(IS_PUBLIC, JsonPrimitive(stepikRemoteInfo?.isPublic ?: false))
    jsonObject.add(IS_ADAPTIVE, JsonPrimitive(stepikRemoteInfo?.isAdaptive ?: false))
    jsonObject.add(IS_IDEA_COMPATIBLE, JsonPrimitive(stepikRemoteInfo?.isIdeaCompatible ?: false))
    jsonObject.add(ID, JsonPrimitive(stepikRemoteInfo?.id ?: 0))
    jsonObject.add(SECTIONS, gson.toJsonTree(stepikRemoteInfo?.sectionIds ?: Lists.emptyList<Int>()))
    jsonObject.add(INSTRUCTORS, gson.toJsonTree(stepikRemoteInfo?.instructors ?: Lists.emptyList<Int>()))

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
    deserializeRemoteInfo(json, course, gson)
    return course
  }

  private fun deserializeRemoteInfo(json: JsonElement, course: StepikCourse, gson: Gson) {
    val jsonObject = json.asJsonObject
    val remoteInfo = StepikCourseRemoteInfo()
    val isPublic = jsonObject.get(IS_PUBLIC).asBoolean
    val isAdaptive = jsonObject.get(IS_ADAPTIVE).asBoolean
    val isCompatible = jsonObject.get(IS_IDEA_COMPATIBLE).asBoolean
    val id = jsonObject.get(ID).asInt

    val sections = gson.fromJson<MutableList<Int>>(jsonObject.get(SECTIONS), object: TypeToken<MutableList<Int>>(){}.type)
    val instructors = gson.fromJson<MutableList<Int>>(jsonObject.get(INSTRUCTORS), object: TypeToken<MutableList<Int>>(){}.type)
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

  private fun getGson(): Gson {
    return GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(Lesson::class.java, StepikLessonRemoteInfoAdapter(language))
      .registerTypeAdapter(StepikSection::class.java, StepikSectionRemoteInfoAdapter(language))
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
  }
}

class StepikSectionRemoteInfoAdapter(val language: String?) : JsonDeserializer<StepikSection>, JsonSerializer<StepikSection> {
  private val ID = "id"
  private val COURSE_ID = "course"
  private val POSITION = "position"
  private val UNITS = "units"
  private val UPDATE_DATE = "update_date"

  override fun serialize(section: StepikSection?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
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

  override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): StepikSection {
    val gson = getGson()

    val section = gson.fromJson(json, StepikSection::class.java)
    deserializeRemoteInfo(gson, section, json)
    return section
  }

  private fun deserializeRemoteInfo(gson: Gson, section: StepikSection, json: JsonElement): Section? {
    val jsonObject = json.asJsonObject

    val remoteInfo = section.stepikRemoteInfo
    val id = jsonObject.get(ID).asInt
    val courseId = jsonObject.get(COURSE_ID).asInt
    val position = jsonObject.get(POSITION).asInt
    val updateDate = gson.fromJson(jsonObject.get(UPDATE_DATE), Date::class.java)
    val units = gson.fromJson<MutableList<Int>>(jsonObject.get(UNITS), object: TypeToken<MutableList<Int>>(){}.type)

    remoteInfo.id = id
    remoteInfo.courseId = courseId
    remoteInfo.position = position
    remoteInfo.updateDate = updateDate
    remoteInfo.units = units

    section.remoteInfo = remoteInfo
    return section
  }

  private fun getGson(): Gson {
    return GsonBuilder()
      .setPrettyPrinting()
      .excludeFieldsWithoutExposeAnnotation()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .registerTypeAdapter(Lesson::class.java, StepikLessonRemoteInfoAdapter(language))
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
  }
}

class StepikLessonRemoteInfoAdapter(val language: String?) : JsonDeserializer<Lesson>, JsonSerializer<Lesson> {
  private val ID = "id"
  private val UPDATE_DATE = "update_date"
  private val IS_PUBLIC = "is_public"
  private val STEPS = "steps"

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
    deserializeRemoteInfo(gson, lesson, json)
    renameAdditionalInfo(lesson)
    return lesson
  }

  private fun deserializeRemoteInfo(gson: Gson, lesson: Lesson, json: JsonElement): Lesson? {
    val jsonObject = json.asJsonObject

    val remoteInfo = StepikLessonRemoteInfo()

    val id = jsonObject.get(ID).asInt
    val isPublic = jsonObject.get(IS_PUBLIC).asBoolean
    val updateDate = gson.fromJson(jsonObject.get(UPDATE_DATE), Date::class.java)
    val steps = gson.fromJson<MutableList<Int>>(jsonObject.get(STEPS), object: TypeToken<MutableList<Int>>(){}.type)

    remoteInfo.id = id
    remoteInfo.isPublic = isPublic
    remoteInfo.updateDate = updateDate
    remoteInfo.steps = steps

    lesson.remoteInfo = remoteInfo
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
      .registerTypeAdapter(Task::class.java, StepikTaskRemoteInfoAdapter())
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
      .create()
  }
}

class StepikTaskRemoteInfoAdapter : JsonDeserializer<Task>, JsonSerializer<Task> {
  private val ID = "stepic_id"
  private val UPDATE_DATE = "update_date"

  override fun serialize(task: Task?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
    val gson = getGson()
    val tree = gson.toJsonTree(task)
    val jsonObject = tree.asJsonObject
    val remoteInfo = task?.remoteInfo

    val stepikRemoteInfo = remoteInfo as? StepikTaskRemoteInfo

    jsonObject.add(ID, JsonPrimitive(stepikRemoteInfo?.stepId ?: 0))

    val updateDate = stepikRemoteInfo?.updateDate
    if (updateDate != null) {
      val date = gson.toJsonTree(updateDate)
      jsonObject.add(UPDATE_DATE, date)
    }

    return jsonObject
  }

  override fun deserialize(json: JsonElement, typeOfT: Type?, context: JsonDeserializationContext?): Task {
    val gson = getGson()

    val task = gson.fromJson(json, Task::class.java)
    deserializeRemoteInfo(gson, task, json)
    return task
  }

  private fun deserializeRemoteInfo(gson: Gson, task: Task, json: JsonElement): Task? {
    val jsonObject = json.asJsonObject

    val remoteInfo = StepikTaskRemoteInfo()

    val id = jsonObject.get(ID).asInt
    val updateDate = gson.fromJson(jsonObject.get(UPDATE_DATE), Date::class.java)

    remoteInfo.stepId = id
    remoteInfo.updateDate = updateDate

    task.remoteInfo = remoteInfo
    return task
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