package com.jetbrains.edu.learning

import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.serialization.converter.json.local.To10VersionLocalCourseConverter
import java.io.File
import java.io.IOException

@Throws(IOException::class)
fun createCourseFromJson(pathToJson: String, courseMode: CourseMode): EduCourse {
  val courseJson = File(pathToJson).readText()
  var objectNode = courseMapper.readTree(courseJson) as ObjectNode
  objectNode = To10VersionLocalCourseConverter().convert(objectNode)
  return courseMapper.treeToValue(objectNode, EduCourse::class.java).apply {
    this.courseMode = courseMode.toString()
  }
}

fun newCourse(courseLanguage: Language, courseMode: CourseMode = CourseMode.EDUCATOR, environment: String = ""): Course = EduCourse().apply {
  name = "Test Course"
  description = "Test Description"
  this.courseMode = courseMode.toString()
  this.environment = environment
  language = courseLanguage.id
}

enum class CourseMode {
  STUDENT,
  EDUCATOR;

  override fun toString(): String = when (this) {
    STUDENT -> EduNames.STUDY
    EDUCATOR -> CCUtils.COURSE_MODE
  }
}
