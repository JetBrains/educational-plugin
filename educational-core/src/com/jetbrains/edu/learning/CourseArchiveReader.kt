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
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator.Companion.addStudyItemMixins
import com.jetbrains.edu.coursecreator.actions.mixins.*
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.COURSE_TYPE
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.VERSION
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.encrypt.EncryptionBundle
import com.jetbrains.edu.learning.encrypt.EncryptionModule
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.plugins.PluginInfo
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.converter.json.local.*
import java.io.IOException

private val LOG = Logger.getInstance(EduUtils::class.java.name)

fun readCourseJson(jsonText: String): Course? {
  return try {
    val courseMapper = getCourseMapper()
    val isArchiveEncrypted = isArchiveEncrypted(jsonText, courseMapper)
    courseMapper.configureCourseMapper(isArchiveEncrypted)
    var courseNode = courseMapper.readTree(jsonText) as ObjectNode
    courseNode = migrate(courseNode)
    courseMapper.treeToValue(courseNode)
  }
  catch (e: IOException) {
    LOG.error("Failed to read course json \n" + e.message)
    null
  }
}

private fun isArchiveEncrypted(jsonText: String, courseMapper: ObjectMapper): Boolean {
  val courseNode = courseMapper.readTree(jsonText) as ObjectNode
  val version = courseNode.get(VERSION)?.asInt() ?: error("Format version is null")
  if (version >= 12) return true
  val courseType = courseNode.get(COURSE_TYPE)?.asText()
  return courseType == MARKETPLACE
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
      11 -> converter = To12VersionLocalCourseConverter()
    }
    if (converter != null) {
      jsonObject = converter.convert(jsonObject)
    }
    version++
  }
  return jsonObject
}

fun getCourseMapper(): ObjectMapper {
  val factory = JsonFactory()
  val mapper = ObjectMapper(factory)
  val module = SimpleModule()
  module.addDeserializer(StudyItem::class.java, StudyItemDeserializer())  // TODO: use JsonSubTypes
  module.addDeserializer(Course::class.java, CourseDeserializer())
  mapper.registerModule(module)
  return mapper
}

fun ObjectMapper.configureCourseMapper(isEncrypted: Boolean) {
  if (isEncrypted) {
    registerModule(EncryptionModule(EncryptionBundle.value("aesKey")))
  }
  addStudyItemMixins()
  configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  addMixIn(PluginInfo::class.java, PluginInfoMixin::class.java)
  addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
  addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
  addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
  disable(MapperFeature.AUTO_DETECT_FIELDS)
  disable(MapperFeature.AUTO_DETECT_GETTERS)
  disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
  disable(MapperFeature.AUTO_DETECT_CREATORS)
}
