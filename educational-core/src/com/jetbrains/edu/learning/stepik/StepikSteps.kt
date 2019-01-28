@file:Suppress("unused", "PropertyName", "MemberVisibilityCanBePrivate")

package com.jetbrains.edu.learning.stepik

import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.serialization.SerializationUtils
import java.util.*

class Step {
  var options: StepOptions? = null
  var text: String = ""
  var name = "pycharm"
  var source: StepOptions? = null // used only in POST

  companion object {

    fun fromTask(project: Project, task: Task): Step {
      val step = Step()
      step.text = task.descriptionText
      step.source = StepOptions.fromTask(project, task)
      return step
    }
  }
}

class StepOptions {
  @JsonProperty("task_type")
  var taskType: String? = null

  @JsonProperty("lesson_type")
  var lessonType: String? = null

  @JsonProperty("title")
  var title: String? = null

  @JsonProperty(SerializationUtils.Json.DESCRIPTION_TEXT)
  var descriptionText: String? = null

  @JsonProperty(SerializationUtils.Json.DESCRIPTION_FORMAT)
  var descriptionFormat: DescriptionFormat? = null

  @JsonProperty("feedback_link")
  var myFeedbackLink = FeedbackLink()

  @JsonProperty("files")
  var files: MutableList<TaskFile>? = null

  @JsonProperty("samples")
  var samples: List<List<String>>? = null

  @JsonProperty("execution_memory_limit")
  var executionMemoryLimit: Int? = null

  @JsonProperty("execution_time_limit")
  var executionTimeLimit: Int? = null

  @JsonProperty("code_templates")
  var codeTemplates: Map<String, String>? = null

  @JsonProperty("format_version")
  var formatVersion = JSON_FORMAT_VERSION

  companion object {

    fun fromTask(project: Project, task: Task): StepOptions {
      val source = StepOptions()
      source.title = task.name
      source.descriptionText = task.descriptionText
      source.descriptionFormat = task.descriptionFormat

      setTaskFiles(project, task, source)

      source.taskType = task.taskType
      source.lessonType = if (task.lesson is FrameworkLesson) "framework" else null
      source.myFeedbackLink = task.feedbackLink
      return source
    }

    private fun setTaskFiles(project: Project, task: Task, source: StepOptions) {
      val files = mutableListOf<TaskFile>()
      if (!task.lesson.isAdditional) {
        val taskDir = task.getTaskDir(project)!!
        for ((_, value) in task.taskFiles) {
          ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
              val answerFile = EduUtils.findTaskFileInDir(value, taskDir)
              if (answerFile == null) return@runWriteAction
              val studentTaskFile = EduUtils.createStudentFile(project, answerFile, task)
              if (studentTaskFile == null) return@runWriteAction
              files.add(studentTaskFile)
            }
          }
        }
      }
      else {
        for ((_, value) in task.taskFiles) {
          files.add(value)
        }
      }
      source.files = files
    }
  }
}

class StepSource {
  var id: Int = 0
  var block: Step? = null
  var position: Int = 0
  var lesson: Int = 0
  var progress: String? = null
  var cost = 1
  @JsonProperty("update_date")
  var updateDate: Date? = null

  constructor()

  constructor(project: Project, task: Task, lesson: Int) {
    this.lesson = lesson
    position = task.index
    block = Step.fromTask(project, task)
    if (task.lesson.isAdditional) {
      cost = 0
    }
  }
}
