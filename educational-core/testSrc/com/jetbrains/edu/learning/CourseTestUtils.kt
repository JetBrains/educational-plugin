package com.jetbrains.edu.learning

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.serialization.SerializationUtils
import java.io.File
import java.io.IOException

@Throws(IOException::class)
fun createCourseFromJson(pathToJson: String, courseType: CourseType): Course {
  val courseJson = File(pathToJson).readText()
  val gson = GsonBuilder()
          .registerTypeAdapter(Task::class.java, SerializationUtils.Json.TaskAdapter())
          .registerTypeAdapter(StudyItem::class.java, SerializationUtils.Json.LessonSectionAdapter())
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create()
  return gson.fromJson(courseJson, Course::class.java).apply {
    courseMode = courseType.toString()
  }
}

@Throws(IOException::class)
fun createRemoteCourseFromJson(pathToJson: String, courseType: CourseType): RemoteCourse {
  val courseJson = File(pathToJson).readText()
  val gson = GsonBuilder()
    .registerTypeAdapter(Task::class.java, SerializationUtils.Json.TaskAdapter())
    .registerTypeAdapter(StudyItem::class.java, SerializationUtils.Json.LessonSectionAdapter())
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()
  return gson.fromJson(courseJson, RemoteCourse::class.java).apply {
    courseMode = courseType.toString()
    sectionIds = sections.map { it.id }
  }
}

fun newCourse(courseLanguage: Language, courseType: CourseType = CourseType.EDUCATOR): Course = Course().apply {
  name = "Test Course"
  description = "Test Description"
  courseMode = courseType.toString()
  language = courseLanguage.id
}

enum class CourseType {
  STUDENT,
  EDUCATOR;

  override fun toString(): String = when (this) {
    STUDENT -> EduNames.STUDY
    EDUCATOR -> CCUtils.COURSE_MODE
  }
}
