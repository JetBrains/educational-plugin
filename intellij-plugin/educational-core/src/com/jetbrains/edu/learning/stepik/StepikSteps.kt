@file:Suppress("unused", "PropertyName", "MemberVisibilityCanBePrivate")

package com.jetbrains.edu.learning.stepik

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.FRAMEWORK
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ID
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.NAME
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.TIME
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.findTaskFileInDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.updateDescriptionTextAndFormat
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TEXT
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillAdditionalInfo
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillStepOptions
import com.jetbrains.edu.learning.taskToolWindow.replaceEncodedShortcuts
import com.jetbrains.edu.learning.toStudentFile
import java.util.*

const val SOURCE = "source"
const val TASK_TYPE = "task_type"
const val HYPERSKILL_ADDITIONAL_INFO = "hyperskill"
const val LESSON_TYPE = "lesson_type"
const val SAMPLES = "samples"
const val EXECUTION_MEMORY_LIMIT = "execution_memory_limit"
const val EXECUTION_TIME_LIMIT = "execution_time_limit"
const val LIMITS = "limits"
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
const val VIDEO = "video"
const val THUMBNAIL = "thumbnail"
const val URLS = "urls"
const val QUALITY = "quality"
const val URL = "url"
const val CODE_TEMPLATES_HEADER = "code_templates_header_lines_count"
const val CODE_TEMPLATES_FOOTER = "code_templates_footer_lines_count"

class Step {
  @JsonProperty(TEXT)
  @JsonDeserialize(converter = TaskDescriptionConverter::class)
  var text: String = ""

  @JsonProperty(NAME)
  var name = ""

  @JsonProperty(VIDEO)
  var video: Video? = null

  @JsonProperty(FEEDBACK_CORRECT)
  var feedbackCorrect: String = ""

  @JsonProperty(FEEDBACK_WRONG)
  var feedbackWrong: String = ""

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "name",
                defaultImpl = PyCharmStepOptions::class)
  @JsonSubTypes(JsonSubTypes.Type(PyCharmStepOptions::class, name = PYCHARM),
                JsonSubTypes.Type(ChoiceStepOptions::class, name = CHOICE))
  // this property is named differently in get and post queries, so we need to define different
  // names for getter (POST queries as data is serialized for query payload)
  // and setter (GET queries as data is deserialized from response)
  @get:JsonProperty(SOURCE, access = JsonProperty.Access.READ_ONLY)
  @set:JsonProperty(OPTIONS, access = JsonProperty.Access.WRITE_ONLY)
  var options: StepOptions? = null

  constructor()

  constructor(project: Project, task: Task) {
    task.updateDescriptionTextAndFormat(project)
    text = if (task.descriptionFormat == DescriptionFormat.MD && task.course !is HyperskillCourse) {
      // convert to html because Stepik website can't display markdown
      EduUtilsKt.convertToHtml(task.descriptionText)
    }
    else task.descriptionText

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

class TaskDescriptionConverter : StdConverter<String, String>() {
  override fun convert(value: String?): String {
    return value?.replaceEncodedShortcuts() ?: ""
  }
}

interface StepOptions

// If you need to store non-plugin task/lesson info, please use com.jetbrains.edu.learning.stepik.api.AdditionalLessonInfo
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
  var descriptionFormat: DescriptionFormat = DescriptionFormat.HTML

  @JsonProperty(FILES)
  var files: MutableList<TaskFile>? = null

  @JsonProperty(SAMPLES)
  var samples: List<List<String>>? = null

  @JsonProperty(EXECUTION_MEMORY_LIMIT)
  var executionMemoryLimit: Int? = null

  @JsonProperty(EXECUTION_TIME_LIMIT)
  var executionTimeLimit: Int? = null

  @JsonProperty(LIMITS)
  var limits: Map<String, ExecutionLimit>? = null

  @JsonProperty(CODE_TEMPLATES)
  var codeTemplates: Map<String, String>? = null

  @JsonProperty(FORMAT_VERSION)
  var formatVersion = JSON_FORMAT_VERSION

  @JsonProperty(CUSTOM_NAME)
  var customPresentableName: String? = null

  @JsonProperty(SOLUTION_HIDDEN)
  var solutionHidden: Boolean? = null

  @JsonProperty(CODE_TEMPLATES_HEADER)
  var codeTemplatesHeader: Map<String, Int>? = null

  @JsonProperty(CODE_TEMPLATES_FOOTER)
  var codeTemplatesFooter: Map<String, Int>? = null

  @JsonProperty(HYPERSKILL_ADDITIONAL_INFO)
  var hyperskill: HyperskillAdditionalInfo? = null

  constructor()

  constructor(project: Project, task: Task) {
    title = task.name
    descriptionFormat = task.descriptionFormat
    descriptionText = task.descriptionText

    files = collectTaskFiles(project, task)
    taskType = task.itemType
    lessonType = if (task.lesson is FrameworkLesson) FRAMEWORK else null
    @Suppress("DEPRECATION")
    customPresentableName = task.customPresentableName
    solutionHidden = task.solutionHidden
  }
}

fun PyCharmStepOptions.hasHeaderOrFooter(submissionLanguage: String): Boolean {
  val header = codeTemplatesHeader?.get(submissionLanguage) ?: return false
  val footer = codeTemplatesFooter?.get(submissionLanguage) ?: return false
  return header > 0 || footer > 0
}

fun collectTaskFiles(project: Project, task: Task): MutableList<TaskFile> {
  val files = mutableListOf<TaskFile>()
  val taskDir = task.getDir(project.courseDir) ?: error("Directory for task ${task.name} does not exist")
  for ((_, value) in task.taskFiles) {
    invokeAndWaitIfNeeded {
      runWriteAction {
        val answerFile = value.findTaskFileInDir(taskDir) ?: return@runWriteAction
        val studentTaskFile = answerFile.toStudentFile(project, task) ?: return@runWriteAction
        files.add(studentTaskFile)
      }
    }
  }
  return files
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

  @JsonProperty(CUSTOM_NAME)
  var customPresentableName: String? = null

  constructor()

  constructor(task: ChoiceTask) {
    isMultipleChoice = task.isMultipleChoice
    sampleSize = task.choiceOptions.size
    customPresentableName = task.presentableName
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

val ChoiceStepOption.choiceStatus: ChoiceOptionStatus get() = if (isCorrect) ChoiceOptionStatus.CORRECT else ChoiceOptionStatus.INCORRECT

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
  open var updateDate: Date = Date(0)

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

class Video {
  @JsonProperty(ID)
  var id: String = ""

  @JsonProperty(THUMBNAIL)
  var thumbnail: String = ""

  @JsonProperty(URLS)
  var listUrls: List<UrlsMap>? = null
}

class UrlsMap {
  @JsonProperty(QUALITY)
  var quality: String = ""

  @JsonProperty(URL)
  var url: String = ""
}

class ExecutionLimit {
  @JsonProperty(TIME)
  var time: Int? = null

  @JsonProperty(MEMORY)
  var memory: Int? = null
}
