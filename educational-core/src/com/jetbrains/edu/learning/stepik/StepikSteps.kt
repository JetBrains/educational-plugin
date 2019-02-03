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
import com.jetbrains.edu.learning.stepik.api.*
import java.util.*

const val SOURCE = "source"
const val TASK_TYPE = "task_type"
const val LESSON_TYPE = "lesson_type"
const val FEEDBACK_LINK = "feedback_link"
const val SAMPLES = "samples"
const val EXECUTION_MEMORY_LIMIT = "execution_memory_limit"
const val EXECUTION_TIME_LIMIT = "execution_time_limit"
const val CODE_TEMPLATES = "code_templates"
const val FORMAT_VERSION = "format_version"
const val BLOCK = "block"
const val PROGRESS = "progress"
const val COST = "cost"

class Step {
  @JsonProperty(OPTIONS)
  var options: StepOptions? = null

  @JsonProperty(TEXT)
  var text: String = ""

  @JsonProperty(NAME)
  var name = "pycharm"

  @JsonProperty(SOURCE)
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
  @JsonProperty(TASK_TYPE)
  var taskType: String? = null

  @JsonProperty(LESSON_TYPE)
  var lessonType: String? = null

  @JsonProperty(TITLE)
  var title: String? = null

  @JsonProperty(SerializationUtils.Json.DESCRIPTION_TEXT)
  var descriptionText: String? = null

  @JsonProperty(SerializationUtils.Json.DESCRIPTION_FORMAT)
  var descriptionFormat: DescriptionFormat? = null

  @JsonProperty(FEEDBACK_LINK)
  var myFeedbackLink = FeedbackLink()

  @JsonProperty(FILES)
  var files: MutableList<TaskFile>? = null

  @JsonProperty(SAMPLES)
  var samples: List<List<String>>? = null

  @JsonProperty(EXECUTION_MEMORY_LIMIT)
  var executionMemoryLimit: Int? = null

  @JsonProperty(EXECUTION_TIME_LIMIT)
  var executionTimeLimit: Int? = null

  @JsonProperty(CODE_TEMPLATES)
  var codeTemplates: Map<String, String>? = null

  @JsonProperty(FORMAT_VERSION)
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
      source.files = files
    }
  }
}

class StepSource {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(BLOCK)
  var block: Step? = null

  @JsonProperty(POSITION)
  var position: Int = 0

  @JsonProperty(LESSON)
  var lesson: Int = 0

  @JsonProperty(PROGRESS)
  var progress: String? = null

  @JsonProperty(COST)
  var cost = 1

  @JsonProperty(UPDATE_DATE)
  var updateDate: Date? = null

  constructor()

  constructor(project: Project, task: Task, lesson: Int) {
    this.lesson = lesson
    position = task.index
    block = Step.fromTask(project, task)
  }
}
