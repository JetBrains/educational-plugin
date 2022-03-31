package com.jetbrains.edu.learning

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.ID
import com.jetbrains.edu.coursecreator.actions.mixins.JsonMixinNames.UPDATE_DATE
import com.jetbrains.edu.coursecreator.actions.mixins.LocalTaskMixin
import com.jetbrains.edu.coursecreator.actions.mixins.RemoteLessonMixin
import com.jetbrains.edu.coursecreator.actions.mixins.RemoteSectionMixin
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.serialization.converter.json.local.To10VersionLocalCourseConverter
import com.jetbrains.edu.learning.stepik.api.STEPIK_ID
import java.io.File
import java.io.IOException
import java.util.*

@Throws(IOException::class)
fun createCourseFromJson(pathToJson: String, courseMode: CourseMode, isEncrypted: Boolean = false): EduCourse {
  val courseJson = File(pathToJson).readText()
  val courseMapper = getCourseMapper()
  configureCourseMapper(courseMapper, isEncrypted)
  var objectNode = courseMapper.readTree(courseJson) as ObjectNode
  objectNode = To10VersionLocalCourseConverter().convert(objectNode)
  return courseMapper.treeToValue(objectNode, EduCourse::class.java).apply {
    this.courseMode = courseMode
  }
}

private fun configureCourseMapper(courseMapper: ObjectMapper, isEncrypted: Boolean) {
  courseMapper.configureCourseMapper(isEncrypted)
  courseMapper.addMixIn(Section::class.java, TestRemoteSectionMixin::class.java)
  courseMapper.addMixIn(Lesson::class.java, TestRemoteLessonMixin::class.java)
  courseMapper.addMixIn(Task::class.java, TestRemoteTaskMixin::class.java)
}

fun newCourse(courseLanguage: Language, courseMode: CourseMode = CourseMode.EDUCATOR, environment: String = ""): Course = EduCourse().apply {
  name = "Test Course"
  description = "Test Description"
  this.courseMode = courseMode
  this.environment = environment
  programmingLanguage = courseLanguage.id
}

@Suppress("UNUSED_PARAMETER", "unused") // used for correct updateDate deserialization from json test data
abstract class TestRemoteLessonMixin : RemoteLessonMixin() {
  @JsonProperty(UPDATE_DATE)
  private lateinit var updateDate: Date
}

@Suppress("UNUSED_PARAMETER", "unused") // used for correct updateDate deserialization from json test data
abstract class TestRemoteTaskMixin : LocalTaskMixin() {
  @JsonProperty(ID)
  @JsonAlias(STEPIK_ID)
  private var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  private lateinit var updateDate: Date
}

@Suppress("UNUSED_PARAMETER", "unused") // used for correct updateDate deserialization from json test data
abstract class TestRemoteSectionMixin : RemoteSectionMixin() {
  @JsonProperty(UPDATE_DATE)
  private lateinit var updateDate: Date
}
