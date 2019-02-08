@file:Suppress("unused", "PropertyName", "MemberVisibilityCanBePrivate")

package com.jetbrains.edu.learning.stepik

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames.FRAMEWORK
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
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
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
const val IS_ALWAYS_CORRECT = "is_always_correct"
const val SAMPLE_SIZE = "sample_size"
const val PRESERVE_ORDER = "preserve_order"
const val IS_HTML_ENABLED = "is_html_enabled"
const val IS_OPTIONS_FEEDBACK = "is_options_feedback"
const val FEEDBACK = "feedback"
const val IS_CORRECT = "is_correct"
const val FORMAT_VERSION = "format_version"
const val BLOCK = "block"
const val PROGRESS = "progress"
const val COST = "cost"
const val PYCHARM = "pycharm"
const val CHOICE = "choice"
const val FEEDBACK_CORRECT = "feedback_correct"
const val FEEDBACK_WRONG = "feedback_wrong"

class Step {
  @JsonProperty(TEXT)
  var text: String = ""

  @JsonProperty(NAME)
  var name = ""

  @JsonProperty(FEEDBACK_CORRECT)
  var feedbackCorrect: String = ""

  @JsonProperty(FEEDBACK_WRONG)
  var feedbackWrong: String = ""

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "name",
                defaultImpl = PyCharmStepOptions::class)
  @JsonSubTypes(JsonSubTypes.Type(PyCharmStepOptions::class, name = PYCHARM),
                JsonSubTypes.Type(ChoiceStepOptions::class, name = CHOICE))
  // this property is named differently in get and post queries so we need to define different
  // names for getter (POST queries as data is serialized for query payload)
  // and setter (GET queries as data is deserialized from response)
  @get:JsonProperty(SOURCE, access = JsonProperty.Access.READ_ONLY)
  @set:JsonProperty(OPTIONS, access = JsonProperty.Access.WRITE_ONLY)
  var options: StepOptions? = null

  constructor()

  constructor(project: Project, task: Task) {
    text = task.descriptionText
    name = if (task is ChoiceTask) CHOICE else PYCHARM
    if (task is ChoiceTask) {
      feedbackCorrect = task.messageCorrect
      feedbackWrong = task.messageIncorrect
    }
    options = when {
      task is ChoiceTask -> ChoiceStepOptions(task)
      task.course is HyperskillCourse -> HyperskillStepOptions(project, task)
      else -> PyCharmStepOptions(project, task)
    }
  }
}

class HyperskillAdditionalInfo {
  @JsonProperty(FILES)
  var files: List<TaskFile>? = null
}

interface StepOptions

open class PyCharmStepOptions : StepOptions {
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
    descriptionFormat = task.descriptionFormat

    setTaskFiles(project, task)

    taskType = task.itemType
    lessonType = if (task.lesson is FrameworkLesson) FRAMEWORK else null
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

class HyperskillStepOptions : PyCharmStepOptions {
  @JsonProperty(HYPERSKILL)
  var hyperskill: HyperskillAdditionalInfo? = null

  constructor()

  constructor(project: Project, task: Task) : super(project, task) {
    val hyperskillAdditionalInfo = HyperskillAdditionalInfo()
    hyperskillAdditionalInfo.files = CCUtils.collectAdditionalFiles(task.course, project)
    hyperskill = hyperskillAdditionalInfo
  }

}

class ChoiceStepOptions : StepOptions {
  @JsonProperty(IS_MULTIPLE_CHOICE)
  var isMultipleChoice = false

  @JsonProperty(IS_ALWAYS_CORRECT)
  val isAlwaysCorrect = false

  @JsonProperty(SAMPLE_SIZE)
  var sampleSize = -1

  @JsonProperty(PRESERVE_ORDER)
  val preserveOrder = true

  @JsonProperty(IS_HTML_ENABLED)
  val isHtmlEnabled = true

  @JsonProperty(IS_OPTIONS_FEEDBACK)
  val isOptionsFeedback = false

  @JsonProperty(OPTIONS)
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
  @JsonProperty(TEXT)
  var text = ""

  @JsonProperty(IS_CORRECT)
  var isCorrect = false

  @JsonProperty(FEEDBACK)
  val feedback = ""
}

open class StepSource {
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

class ChoiceStepSource {
  @JsonProperty(BLOCK)
  var block: ChoiceStep? = null
}

class ChoiceStep {
  @JsonProperty(FEEDBACK_CORRECT)
  var feedbackCorrect: String = ""

  @JsonProperty(FEEDBACK_WRONG)
  var feedbackWrong: String = ""

  @JsonProperty(SOURCE)
  var source: ChoiceStepOptions? = null
}
