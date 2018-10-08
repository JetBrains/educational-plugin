package com.jetbrains.edu.learning.stepik.alt

import com.intellij.lang.Language
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.stepik.StepikConnector

const val LINK_TEMPLATE = HYPERSKILL_URL + "learn/lesson/"

fun getLesson(lessonId: Int,
              language: Language,
              stages: List<HyperskillStage>): Lesson? {
  val lesson = StepikConnector.getLesson(lessonId)
  val stepIds = lesson.steps.map { stepId -> stepId.toString() }.toTypedArray()
  val allStepSources = StepikConnector.getStepSources(stepIds, language.displayName)
  val tasks = StepikConnector.getTasks(language, stepIds, allStepSources)
  val convertedTasks = mutableListOf<EduTask>()
  for ((index, task) in tasks.withIndex()) {
    val stage = stages[index]
    val eduTask = EduTask(stage.title)
    // TODO: extract as template
    eduTask.addTaskFile(TaskFile("src/Task.java", ""))
    eduTask.descriptionText = task.descriptionText
    eduTask.feedbackLink = FeedbackLink(LINK_TEMPLATE + lesson.id)
    convertedTasks.add(eduTask)
  }

  lesson.taskList.addAll(convertedTasks)
  return lesson
}