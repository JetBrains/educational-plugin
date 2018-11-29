package com.jetbrains.edu.learning

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.serialization.SerializationUtils
import java.io.File
import java.io.IOException

@Throws(IOException::class)
fun createCourseFromJson(pathToJson: String, courseMode: CourseMode): Course {
  val courseJson = File(pathToJson).readText()
  val gson = GsonBuilder()
          .registerTypeAdapter(Task::class.java, SerializationUtils.Json.TaskAdapter())
          .registerTypeAdapter(StudyItem::class.java, SerializationUtils.Json.LessonSectionAdapter())
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create()
  return gson.fromJson(courseJson, EduCourse::class.java).apply {
    this.courseMode = courseMode.toString()
  }
}

@Throws(IOException::class)
fun createRemoteCourseFromJson(pathToJson: String, courseMode: CourseMode): EduCourse {
  val courseJson = File(pathToJson).readText()
  val gson = GsonBuilder()
    .registerTypeAdapter(Task::class.java, SerializationUtils.Json.TaskAdapter())
    .registerTypeAdapter(StudyItem::class.java, SerializationUtils.Json.LessonSectionAdapter())
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .create()
  return gson.fromJson(courseJson, EduCourse::class.java).apply {
    this.courseMode = courseMode.toString()
  }
}

fun newCourse(courseLanguage: Language, courseMode: CourseMode = CourseMode.EDUCATOR, courseType: String = EduNames.PYCHARM): Course = EduCourse().apply {
  name = "Test Course"
  description = "Test Description"
  this.courseMode = courseMode.toString()
  this.courseType = courseType
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
