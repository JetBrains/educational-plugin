package com.jetbrains.edu.learning.stepik

import com.google.gson.GsonBuilder
import com.intellij.lang.Language
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.VideoTaskResourcesManager
import org.jetbrains.annotations.NonNls
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import java.util.Collections.unmodifiableList

// TODO: get rid of LeakingThis warnings without suppression
@Suppress("LeakingThis")
open class StepikTaskBuilder(
  course: Course,
  private val lesson: Lesson,
  private val stepSource: StepSource,
  private val stepId: Int,
  private val userId: Int
) {
  private val courseType: String = course.itemType
  private val courseEnvironment: String = course.environment
  private val language: Language = course.languageById ?: Language.ANY
  private val step: Step = stepSource.block ?: error("Step is empty")

  private val stepikTaskTypes: Map<String, (String) -> Task> = mapOf(
    "code" to this::codeTask,
    "choice" to this::choiceTask,
    "text" to this::theoryTask,
    "string" to this::theoryTask,
    "pycharm" to { _: String -> pycharmTask() },
    "video" to this::videoTask,
    "number" to this::unsupportedTask,
    "sorting" to this::unsupportedTask,
    "matching" to this::unsupportedTask,
    "math" to this::unsupportedTask,
    "free-answer" to this::unsupportedTask,
    "table" to this::unsupportedTask,
    "dataset" to this::unsupportedTask,
    "admin" to this::unsupportedTask,
    "manual-score" to this::unsupportedTask
  )

  private val pluginTaskTypes: Map<String, (String) -> Task> = mapOf(
    "edu" to { name: String -> EduTask(name) },
    "output" to { name: String -> OutputTask(name) },
    "ide" to { name: String -> IdeTask(name) },
    "theory" to this::theoryTask
  )

  fun createTask(type: String): Task? {
    val taskName = DEFAULT_NAMES[type]
    return stepikTaskTypes[type]?.invoke(taskName ?: UNKNOWN_TASK_NAME)
  }

  fun isSupported(type: String): Boolean {
    return stepikTaskTypes.containsKey(type)
  }

  private fun codeTask(name: String): CodeTask {
    val task = CodeTask(name)
    task.id = stepId
    task.index = stepSource.position
    task.updateDate = stepSource.updateDate

    task.status = CheckStatus.Unchecked
    val options = step.options as PyCharmStepOptions
    val samples = options.samples

    task.descriptionText = buildString {
      append(clearCodeBlockFromTags())

      if (samples != null) {
        append("<br>")
        for (sample in samples) {
          if (sample.size == 2) {
            append("<b>Sample Input:</b><br>${sample[0].replace("\n", "<br>")}<br>")
            append("<b>Sample Output:</b><br>${sample[1].replace("\n", "<br>")}<br><br>")
          }
        }
      }
      val memoryLimit = options.executionMemoryLimit
      val timeLimit = options.executionTimeLimit
      if (memoryLimit != null && timeLimit != null) {
        append("<br><font color=\"gray\">Memory limit: $memoryLimit Mb</font>")
        append("<br><font color=\"gray\">Time limit: ${timeLimit}s</font><br><br>")
      }
    }

    if (language.isKindOf(EduNames.PYTHON) && samples != null) {
      createTestFileFromSamples(task, samples)
    }
    createMockTaskFile(task, "write your answer here \n", getCodeTemplateForTask(options.codeTemplates))
    return task
  }

  private fun clearCodeBlockFromTags(): String {
    val parsedText = Jsoup.parse(step.text)
    for (element in parsedText.select("code")) {
      val settings = Document.OutputSettings().prettyPrint(false)
      var codeBlockWithoutTags = Jsoup.clean(element.html(), "", Whitelist().addTags("br"), settings)
      codeBlockWithoutTags = codeBlockWithoutTags.replace("<br>", "\n")
      codeBlockWithoutTags = codeBlockWithoutTags.replace("[\n]+".toRegex(), "\n")
      element.html(codeBlockWithoutTags)
    }
    return parsedText.toString()
  }

  private fun choiceTask(name: String): ChoiceTask {
    val task = ChoiceTask(name)
    task.id = stepId
    task.index = stepSource.position
    task.updateDate = stepSource.updateDate
    task.descriptionText = clearCodeBlockFromTags()

    val choiceStep: ChoiceStep? = if (!isUnitTestMode || stepId > 0)
      StepikConnector.getInstance().getChoiceStepSource(stepId)
    else null

    if (choiceStep != null) {
      val choiceStepOptions = choiceStep.source
      if (choiceStepOptions != null) {
        task.isMultipleChoice = choiceStepOptions.isMultipleChoice
        task.choiceOptions = choiceStepOptions.options.map { ChoiceOption(it.text, it.choiceStatus) }
      }
      if (choiceStep.feedbackCorrect.isNotEmpty()) {
        task.messageCorrect = choiceStep.feedbackCorrect
      }
      if (choiceStep.feedbackWrong.isNotEmpty()) {
        task.messageIncorrect = choiceStep.feedbackWrong
      }
    }
    else if (!isUnitTestMode) {
      val attempt = StepikCheckerConnector.getAttemptForStep(stepId, userId)
      if (attempt != null) {
        val dataset = attempt.dataset
        if (dataset?.options != null) {
          task.choiceOptions = dataset.options.orEmpty().map(::ChoiceOption)
          task.isMultipleChoice = dataset.isMultipleChoice
        }
        else {
          LOG.warn("Dataset for step $stepId is null")
        }
      }
    }

    createMockTaskFile(task)
    return task
  }

  private fun theoryTask(name: String): TheoryTask {
    val task = TheoryTask(name)
    task.id = stepId
    task.index = stepSource.position
    task.updateDate = stepSource.updateDate
    task.descriptionText = clearCodeBlockFromTags()

    createMockTaskFile(task)
    return task
  }

  private fun videoTask(name: String): VideoTask {
    val task = VideoTask(name)
    task.id = stepId
    task.index = stepSource.position
    task.updateDate = stepSource.updateDate
    var descriptionText = "View this video on <a href=\"${getStepikLink(task, lesson)}\">Stepik</a>."
    val block = stepSource.block
    if (block != null) {
      val video = block.video
      if (video != null) {
        task.thumbnail = video.thumbnail
        task.sources = unmodifiableList(video.listUrls?.map { VideoSource(it.url, it.quality) } ?: emptyList())
        descriptionText = VideoTaskResourcesManager().getText(task, lesson)
      }
      else {
        LOG.warn("Video for step $stepId is null")
      }
    }
    else {
      LOG.warn("Block for step $stepId is null")
    }

    task.descriptionText = descriptionText
    createMockTaskFile(task)
    return task
  }

  private fun unsupportedTask(@NonNls name: String): Task {
    val task = TheoryTask(name)
    task.id = stepId
    task.index = stepSource.position
    task.updateDate = stepSource.updateDate
    task.descriptionText = "${name.toLowerCase().capitalize()} tasks are not supported yet. <br>" +
                           "View this step on <a href=\"${getStepikLink(task, lesson)}\">Stepik</a>."

    createMockTaskFile(task, "This is a ${name.toLowerCase()} task. You can use this editor as a playground\n")
    return task
  }

  private fun pycharmTask(): Task {
    val stepOptions = step.options as PyCharmStepOptions
    val taskName: String = stepOptions.title ?: DEFAULT_EDU_TASK_NAME

    val task = createPluginTask(taskName)
    task.id = stepId
    task.updateDate = stepSource.updateDate

    task.customPresentableName = stepOptions.customPresentableName
    task.solutionHidden = stepOptions.solutionHidden

    task.descriptionText = if (!stepOptions.descriptionText.isNullOrEmpty()) stepOptions.descriptionText.orEmpty() else step.text
    task.descriptionFormat = stepOptions.descriptionFormat
    task.feedbackLink = stepOptions.myFeedbackLink

    stepOptions.files?.forEach {
      addPlaceholdersTexts(it)
      task.addTaskFile(it)
    }

    return task
  }

  private fun createPluginTask(name: String): Task {
    val options = step.options as PyCharmStepOptions
    return pluginTaskTypes.getOrDefault(options.taskType, null)?.invoke(name) ?: EduTask(name)
  }

  private fun createMockTaskFile(
    task: Task,
    comment: String = "You can experiment here, it won’t be checked\n",
    codeTemplate: String? = null
  ) {
    val options = step.options
    if (options is PyCharmStepOptions && !options.files.isNullOrEmpty()) {
      options.files?.forEach { task.addTaskFile(it) }
      return
    }

    val configurator = EduConfiguratorManager.findConfigurator(courseType, courseEnvironment, language)
    val editorText = buildString {
      if (codeTemplate == null) {
        val commentPrefix = LanguageCommenters.INSTANCE.forLanguage(language)?.lineCommentPrefix
        if (commentPrefix != null) {
          append("$commentPrefix $comment")
        }

        if (configurator != null) {
          append("\n${configurator.mockTemplate}")
        }
      }
      else {
        append(codeTemplate)
      }
    }

    val taskFilePath = getTaskFilePath(editorText, configurator) ?: return
    val taskFile = TaskFile()
    taskFile.setText(editorText)
    taskFile.name = taskFilePath
    task.addTaskFile(taskFile)
  }

  private fun getCodeTemplateForTask(codeTemplates: Map<String, String>?): String? {
    val languageString = getLanguageName(language)
    return codeTemplates?.get(languageString)
  }

  protected open fun getLanguageName(language: Language): String? {
    return StepikLanguages.langOfId(language.id).langName
  }

  companion object {
    private val LOG = Logger.getInstance(StepikTaskBuilder::class.java)

    private val DEFAULT_NAMES: Map<String, String> = mapOf(
      "code" to "Programming",
      "choice" to "Quiz",
      "text" to "Theory",
      "pycharm" to "Programming",
      "video" to "Video",
      "number" to "Number",
      "sorting" to "Sorting",
      "matching" to "Matching",
      "string" to "Text",
      "math" to "Math",
      "free-answer" to "Free Response",
      "table" to "Table",
      "dataset" to "Data",
      "admin" to "Linux",
      "manual-score" to "Manual Score"
    )

    private const val DEFAULT_EDU_TASK_NAME = "Edu Task"
    private const val UNKNOWN_TASK_NAME = "Unknown Task"

    private fun addPlaceholdersTexts(file: TaskFile) {
      val fileText = file.text
      for (placeholder in file.answerPlaceholders) {
        val offset = placeholder.offset
        val length = placeholder.length
        if (fileText.length > offset + length) {
          placeholder.placeholderText = fileText.substring(offset, offset + length)
        }
      }
    }

    private fun getTaskFilePath(editorText: String, configurator: EduConfigurator<*>?): String? {
      val fileName = configurator?.getMockFileName(editorText) ?: return null
      return GeneratorUtils.joinPaths(configurator.sourceDir, fileName)
    }

    private fun createTestFileFromSamples(task: Task, samples: List<List<String>>) {
      val testText =
        "from test_helper import check_samples\n\n" +
        "if __name__ == '__main__':\n" +
        "    check_samples(samples=${GsonBuilder().create().toJson(samples)})"
      val test = TaskFile("tests.py", testText)
      test.isVisible = false
      task.addTaskFile(test)
    }
  }
}
