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
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CourseArchiveCreator
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
import com.jetbrains.edu.learning.taskDescription.replaceEncodedShortcuts
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
const val VIDEO = "video"
const val THUMBNAIL = "thumbnail"
const val URLS = "urls"
const val QUALITY = "quality"
const val URL = "url"

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
  // this property is named differently in get and post queries so we need to define different
  // names for getter (POST queries as data is serialized for query payload)
  // and setter (GET queries as data is deserialized from response)
  @get:JsonProperty(SOURCE, access = JsonProperty.Access.READ_ONLY)
  @set:JsonProperty(OPTIONS, access = JsonProperty.Access.WRITE_ONLY)
  var options: StepOptions? = null

  constructor()

  constructor(project: Project, task: Task) {
    CourseArchiveCreator.addDescriptions(project, task)
    text = if (task.descriptionFormat == DescriptionFormat.MD) {
      val taskDir = task.getTaskDir(project)
      if (taskDir != null) EduUtils.generateMarkdownHtml(taskDir, task.descriptionText) else task.descriptionText
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

  @JsonProperty(CUSTOM_NAME)
  var customPresentableName: String? = null

  @JsonProperty(SOLUTION_HIDDEN)
  var solutionHidden: Boolean? = null

  constructor()

  constructor(project: Project, task: Task) {
    title = task.name
    descriptionFormat = task.descriptionFormat
    descriptionText = task.descriptionText

    files = collectTaskFiles(project, task)
    taskType = task.itemType
    lessonType = if (task.lesson is FrameworkLesson) FRAMEWORK else null
    myFeedbackLink = task.feedbackLink
    @Suppress("deprecation")
    customPresentableName = task.customPresentableName
    solutionHidden = task.solutionHidden
  }
}

fun collectTaskFiles(project: Project, task: Task): MutableList<TaskFile> {
  val files = mutableListOf<TaskFile>()
  val taskDir = task.getTaskDir(project) ?: error("Directory for task ${task.name} does not exist")
  for ((_, value) in task.taskFiles) {
    invokeAndWaitIfNeeded {
      runWriteAction {
        val answerFile = EduUtils.findTaskFileInDir(value, taskDir) ?: return@runWriteAction
        val studentTaskFile = EduUtils.createStudentFile(project, answerFile, task) ?: return@runWriteAction
        files.add(studentTaskFile)
      }
    }
  }
  return files
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
  open var updateDate: Date? = null

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
