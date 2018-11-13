package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.lang.Language
import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.stepik.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

fun getLesson(course: HyperskillCourse, lessonId: Int, language: Language, stages: List<HyperskillStage>): Lesson? {
  val progressIndicator = ProgressManager.getInstance().progressIndicator
  val lesson = StepikConnector.getLesson(lessonId)
  lesson.course = course
  progressIndicator?.checkCanceled()
  progressIndicator?.text2 = "Loading project steps"
  val stepIds = lesson.steps.map { stepId -> stepId.toString() }.toTypedArray()
  val allStepSources = StepikConnector.getStepSources(stepIds, language.baseLanguage?.id)
  progressIndicator?.checkCanceled()
  progressIndicator?.text2 = "Loading tasks"
  val tasks = StepikConnector.getTasks(language, stepIds, allStepSources)
  progressIndicator?.checkCanceled()
  progressIndicator?.text2 = "Loading topics"
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
