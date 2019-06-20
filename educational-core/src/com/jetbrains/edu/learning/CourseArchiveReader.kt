@file:JvmName("CourseArchiveReader")
package com.jetbrains.edu.learning

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.coursecreator.actions.mixins.*
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.converter.json.local.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private val LOG = Logger.getInstance(EduUtils::class.java.name)

fun readCourseJson(jsonText: String): Course? {
  return try {
    var courseNode = courseMapper.readTree(jsonText) as ObjectNode
    courseNode = migrate(courseNode)
    courseMapper.treeToValue(courseNode)
  }
  catch (e: IOException) {
    LOG.error("Failed to read course json \n" + e.message)
    null
  }
}

private fun migrate(jsonObject: ObjectNode): ObjectNode {
  return migrate(jsonObject, JSON_FORMAT_VERSION)
}

@VisibleForTesting
fun migrate(node: ObjectNode, maxVersion: Int): ObjectNode {
  var jsonObject = node
  val jsonVersion = jsonObject.get(SerializationUtils.Json.VERSION)
  var version: Int
  version = jsonVersion?.asInt() ?: 1

  while (version < maxVersion) {
    var converter: JsonLocalCourseConverter? = null
    when (version) {
      6 -> converter = ToSeventhVersionLocalCourseConverter()
      7 -> converter = To8VersionLocalCourseConverter()
      8 -> converter = To9VersionLocalCourseConverter()
      9 -> converter = To10VersionLocalCourseConverter()
      10 -> converter = To11VersionLocalCourseConverter()
    }
    if (converter != null) {
      jsonObject = converter.convert(jsonObject)
    }
    version++
  }
  return jsonObject
}

val courseMapper: ObjectMapper  // TODO: common mapper for archive creator and reader?
  get() {
    val factory = JsonFactory()
    val mapper = ObjectMapper(factory)
    val module = SimpleModule()
    module.addDeserializer(StudyItem::class.java, StudyItemDeserializer())  // TODO: use JsonSubTypes
    module.addDeserializer(Course::class.java, CourseDeserializer())
    mapper.registerModule(module)
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    mapper.addMixIn(CourseraCourse::class.java, CourseraCourseMixin::class.java)
    mapper.addMixIn(EduCourse::class.java, RemoteEduCourseMixin::class.java)
    mapper.addMixIn(Section::class.java, RemoteSectionMixin::class.java)
    mapper.addMixIn(Lesson::class.java, RemoteLessonMixin::class.java)
    mapper.addMixIn(Task::class.java, RemoteTaskMixin::class.java)
    mapper.addMixIn(ChoiceTask::class.java, ChoiceTaskLocalMixin::class.java)
    mapper.addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
    mapper.addMixIn(FeedbackLink::class.java, FeedbackLinkMixin::class.java)
    mapper.addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
    mapper.addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
    mapper.disable(MapperFeature.AUTO_DETECT_FIELDS)
    mapper.disable(MapperFeature.AUTO_DETECT_GETTERS)
    mapper.disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
    mapper.disable(MapperFeature.AUTO_DETECT_CREATORS)
    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH)
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    mapper.dateFormat = dateFormat
    return mapper
  }
