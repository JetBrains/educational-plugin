package com.jetbrains.edu.learning.stepik.alt

import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.stepik.StepikConnector

fun getLesson(lessonId: Int, language: Language, stages: List<HyperskillStage>): Lesson? {
  val lesson = StepikConnector.getLesson(lessonId)
  val stepIds = lesson.steps.map { stepId -> stepId.toString() }.toTypedArray()
  val allStepSources = StepikConnector.getStepSources(stepIds, language.baseLanguage?.id)
  val tasks = StepikConnector.getTasks(language, stepIds, allStepSources)
  val convertedTasks = mutableListOf<EduTask>()
  for ((index, task) in tasks.withIndex()) {
    val stage = stages[index]
    val eduTask = EduTask(stage.title)
    // TODO: extract as template or clone repository
    eduTask.addTaskFile(TaskFile("src/Task.java", ""))
    eduTask.descriptionText = task.descriptionText
    convertedTasks.add(eduTask)
  }

  lesson.taskList.addAll(convertedTasks)
  return lesson
}