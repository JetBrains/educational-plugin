package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.lang.Language
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader
import com.jetbrains.edu.learning.stepik.api.loadAttachment
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

fun getLesson(course: HyperskillCourse, lessonId: Int, language: Language): Lesson? {
  val progressIndicator = ProgressManager.getInstance().progressIndicator
  var lesson = StepikConnector.getLesson(lessonId)
  if (lesson == null) return null
  lesson.course = course
  lesson = FrameworkLesson(lesson)
  progressIndicator?.checkCanceled()
  progressIndicator?.text2 = "Loading project stages"
  val stepSources = HyperskillConnector.getStepSources(lessonId) ?: return null
  progressIndicator?.checkCanceled()
  val tasks = StepikCourseLoader.getTasks(language, lesson, stepSources)
  lesson.taskList.addAll(tasks)
  loadAttachment(course, lesson)
  return lesson
}
