package com.jetbrains.edu.learning

import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import java.io.File
import java.io.IOException

@Throws(IOException::class)
fun createCourseFromJson(pathToJson: String): Course {
  val courseJson = File(pathToJson).readText()
  val gson = GsonBuilder()
          .registerTypeAdapter(Task::class.java, SerializationUtils.Json.TaskAdapter())
          .registerTypeAdapter(Lesson::class.java, SerializationUtils.Json.LessonAdapter())
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create()
  return gson.fromJson(courseJson, Course::class.java)
}

fun newCourse(courseLanguage: Language): Course = Course().apply {
  name = "Test Course"
  description = "Test Description"
  courseMode = CCUtils.COURSE_MODE
  language = courseLanguage.id
}
