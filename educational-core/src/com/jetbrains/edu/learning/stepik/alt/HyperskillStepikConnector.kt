package com.jetbrains.edu.learning.stepik.alt

import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.StepikConnector
import com.jetbrains.edu.learning.stepik.StepikConnector.*
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikWrappers

const val LINK_TEMPLATE = HYPERSKILL_URL + "learn/lesson/"

fun getLesson(lessonId: Int, language: Language): Lesson? {
  val lesson = StepikConnector.getLesson(lessonId)
  val stepIds = lesson.steps.map { stepId -> stepId.toString() }.toTypedArray()
  val allStepSources = StepikConnector.getStepSources(stepIds, language.displayName)
  val tasks = StepikConnector.getTasks(language, stepIds, allStepSources)
  tasks.forEach { task ->
    task.feedbackLink = FeedbackLink(LINK_TEMPLATE + lesson.id)
  }
  lesson.taskList.addAll(tasks)
  return lesson
}

fun getLessons(lessonIds: List<String>, languageId: String): List<Lesson> {
  val lessons = multipleRequestToStepik<StepikWrappers.LessonContainer>(
    StepikNames.LESSONS, lessonIds.toTypedArray(), StepikWrappers.LessonContainer::class.java).flatMap { it -> it.lessons }

  val language = Language.findLanguageByID(languageId)

  lessons.forEach {
    val stepIds = it.steps.map { stepId -> stepId.toString() }.toTypedArray()
    val allStepSources = getStepSources(stepIds, languageId)
    val tasks = getTasks(language, stepIds, allStepSources)
    tasks.forEach { task ->
      task.feedbackLink = FeedbackLink(LINK_TEMPLATE + it.id)
    }
    it.taskList.addAll(tasks)
  }
  return lessons
}
