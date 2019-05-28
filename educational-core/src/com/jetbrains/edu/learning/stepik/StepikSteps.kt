@file:Suppress("unused", "PropertyName", "MemberVisibilityCanBePrivate")

package com.jetbrains.edu.learning.stepik

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.stepik.api.*
import java.util.*

const val SOURCE = "source"
const val TASK_TYPE = "task_type"
const val HYPERSKILL = "hyperskill"
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
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "name",
                defaultImpl = PyCharmStepOptions::class)
  @JsonSubTypes(JsonSubTypes.Type(PyCharmStepOptions::class, name = "pycharm"),
                JsonSubTypes.Type(ChoiceStepOptions::class, name = "choice"))
  @JsonProperty(OPTIONS)
  var options: StepOptions? = null

  @JsonProperty(TEXT)
  var text: String = ""

  @JsonProperty(NAME)
  var name = ""

  @JsonProperty(SOURCE)
  var source: StepOptions? = null // used only in POST

  constructor()

  constructor(project: Project, task: Task) {
    text = task.descriptionText
    name = if (task is ChoiceTask) "choice" else "pycharm"
    source = when (task) {
      is ChoiceTask -> ChoiceStepOptions(task)
      else -> PyCharmStepOptions(project, task)
    }
  }
}

interface StepOptions

class PyCharmStepOptions : StepOptions {
  @JsonProperty(HYPERSKILL)
  var hyperskill: Any? = null

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

  constructor()

  constructor(project: Project, task: Task) {
    title = task.name
    descriptionText = task.descriptionText
    descriptionFormat = task.descriptionFormat

    setTaskFiles(project, task)

    taskType = task.itemType
    lessonType = if (task.lesson is FrameworkLesson) "framework" else null
    myFeedbackLink = task.feedbackLink
  }

  private fun setTaskFiles(project: Project, task: Task) {
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
    this.files = files
  }
}

class ChoiceStepOptions : StepOptions {
  @JsonProperty("is_multiple_choice")
  var isMultipleChoice = false

  @JsonProperty("is_always_correct")
  val isAlwaysCorrect = false

  @JsonProperty("sample_size")
  var sampleSize = -1

  @JsonProperty("preserve_order")
  val preserveOrder = true

  @JsonProperty("is_html_enabled")
  val isHtmlEnabled = true

  @JsonProperty("is_options_feedback")
  val isOptionsFeedback = false

  @JsonProperty("options")
  var options = emptyList<ChoiceStepOption>()

  constructor()

  constructor(task: ChoiceTask) {
    isMultipleChoice = task.isMultipleChoice
    sampleSize = task.choiceOptions.size
    options = task.choiceOptions.map {
      val option = ChoiceStepOption()
      option.text = it.text
      option.isCorrect = it.status == ChoiceOptionStatus.CORRECT
      option
    }
  }
}

class ChoiceStepOption {
  @JsonProperty("text")
  var text = ""

  @JsonProperty("is_correct")
  var isCorrect = false

  @JsonProperty("feedback")
  val feedback = ""
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
    block = Step(project, task)
  }
}
