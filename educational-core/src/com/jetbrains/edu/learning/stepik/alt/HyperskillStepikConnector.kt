package com.jetbrains.edu.learning.stepik.alt

import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.StepikConnector.*
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikWrappers

fun getLessons(lessonIds: List<String>): List<Lesson> {
  val lessons = multipleRequestToStepik<StepikWrappers.LessonContainer>(
    StepikNames.LESSONS, lessonIds.toTypedArray(), StepikWrappers.LessonContainer::class.java).flatMap { it -> it.lessons }

  val language = Language.findLanguageByID("JAVA")

  lessons.forEach {
    val stepIds = it.steps.map { stepId -> stepId.toString() }.toTypedArray()
    val allStepSources = getStepSources(stepIds, "JAVA")
    val tasks = getTasks(language, stepIds, allStepSources)
    it.taskList.addAll(tasks)
  }
  return lessons
}
