package com.jetbrains.edu.assistant.validation.actions.next.step.hint

import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.eduState

data class BaseAssistantInfoStorage(val task: EduTask) {
  val taskProcessor = TaskProcessor(task)
  val assistant = TaskBasedAssistant(taskProcessor)
  val language = task.course.languageById ?: error("Language could not be determined")
  val project get() = task.project ?: error("Cannot get project")

  val eduState get() = project.eduState ?: error("Cannot get eduState for project ${project.name}")
}
