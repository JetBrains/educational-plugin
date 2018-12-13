package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.lang.Language
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

fun getLesson(course: HyperskillCourse, lessonId: Int, language: Language): Lesson? {
  val progressIndicator = ProgressManager.getInstance().progressIndicator
  var lesson = StepikConnector.getLesson(lessonId)
  lesson.course = course
  progressIndicator?.checkCanceled()
  progressIndicator?.text2 = "Loading project steps"
  val stepIds = lesson.steps.map { stepId -> stepId.toString() }.toTypedArray()
  val allStepSources = StepikConnector.getStepSources(stepIds, language.baseLanguage?.id)
  if (allStepSources.isEmpty()) {
    return null
  }
  allStepSources[0].block.options?.lessonType ?: return null
  lesson = FrameworkLesson(lesson)

  progressIndicator?.checkCanceled()
  progressIndicator?.text2 = "Loading tasks"
  val tasks = StepikConnector.getTasks(language, lesson, stepIds, allStepSources)
  lesson.taskList.addAll(tasks)
  return lesson
}
