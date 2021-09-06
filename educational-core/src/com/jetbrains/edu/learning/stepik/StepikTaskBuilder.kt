package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.lang.Language
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil.join
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduExperimentalFeatures.HYPERSKILL_DATA_TASKS_SUPPORT
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask.CODE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.EDU_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.PYCHARM_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask.Companion.IDE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask.Companion.OUTPUT_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask.THEORY_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask.Companion.VIDEO_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask.Companion.CHOICE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_SAMPLE_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.DATA_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.data.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.VideoTaskResourcesManager
import com.jetbrains.edu.learning.xmlEscaped
import org.jetbrains.annotations.NonNls
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import java.util.*
import java.util.Collections.unmodifiableList

open class StepikTaskBuilder(
  course: Course,
  private val lesson: Lesson,
  private val stepSource: StepSource,
  private val stepId: Int,
  private val userId: Int
) {
  private val courseType: String = course.itemType
  private val courseMode: String = course.courseMode
  private val courseEnvironment: String = course.environment
  private val language: Language = course.languageById ?: Language.ANY
  private val languageVersion: String = course.languageVersion ?: ""
  private val step: Step = stepSource.block ?: error("Step is empty")
  private val updateDate = stepSource.updateDate ?: Date(0)

  private val pluginTaskTypes: Map<String, (String) -> Task> = mapOf(
    EDU_TASK_TYPE to { name: String -> EduTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked) },
    OUTPUT_TASK_TYPE to { name: String -> OutputTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked) },
    IDE_TASK_TYPE to { name: String -> IdeTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked) },
    THEORY_TASK_TYPE to { name: String -> TheoryTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked) },
    DATA_TASK_TYPE to { name: String -> DataTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked) }
  )

  private val stepikTaskBuilders: Map<String, (String) -> Task> = StepikTaskType.values().associateBy(
    { it.type },
    {
      when (it) {
        StepikTaskType.CHOICE -> this::choiceTask
        StepikTaskType.CODE -> this::codeTask
        StepikTaskType.PYCHARM -> { _: String -> pycharmTask() }
        StepikTaskType.TEXT -> this::theoryTask
        StepikTaskType.VIDEO -> this::videoTask
        StepikTaskType.DATASET -> this::dataTask
        else -> this::unsupportedTask
      }
    })

  enum class StepikTaskType(val type: String, val value: String) {
    CHOICE(CHOICE_TASK_TYPE, "Quiz"),
    CODE(CODE_TASK_TYPE, "Programming"),
    PYCHARM(PYCHARM_TASK_TYPE, "Programming"),
    TEXT("text", "Theory"),
    VIDEO(VIDEO_TASK_TYPE, "Video"),
    NUMBER("number", "Number"),
    SORTING("sorting", "Sorting"),
    MATCHING("matching", "Matching"),
    STRING("string", "Text"),
    MATH("math", "Math"),
    FREE_ANSWER("free-answer", "Free Response"),
    TABLE("table", "Table"),
    DATASET(DATA_TASK_TYPE, "Data"),
    ADMIN("admin", "Linux"),
    MANUAL_SCORE("manual-score", "Manual Score")
  }

  open fun createTask(type: String): Task? {
    val taskName = StepikTaskType.values().find { it.type == type }?.value ?: UNKNOWN_TASK_NAME
    return stepikTaskBuilders[type]?.invoke(taskName)
  }

  fun isSupported(type: String): Boolean {
    if (type == DATA_TASK_TYPE && !isFeatureEnabled(HYPERSKILL_DATA_TASKS_SUPPORT)) {
      return false
    }
    return stepikTaskBuilders.containsKey(type)
  }

  private fun Step.pycharmOptions(): PyCharmStepOptions {
    return options as PyCharmStepOptions
  }

  private fun Task.fillDescription() {
    if (this !is CodeTask && this !is DataTask) return

    val options = step.pycharmOptions()
    val samples = options.samples

    fun String.prepareSample(): String {
      val replaceBr = replace("\n", "<br>")
      return if (isValidHtml(this)) replaceBr.xmlEscaped else replaceBr
    }

    descriptionText = buildString {
      append(clearCodeBlockFromTags())

      if (samples != null) {
        append("<br>")
        for (sample in samples) {
          if (sample.size == 2) {
            append("<b>Sample Input:</b><br><pre><code class=\"language-no-highlight\">${sample[0].prepareSample()}</code></pre><br>")
            append("<b>Sample Output:</b><br><pre><code class=\"language-no-highlight\">${sample[1].prepareSample()}</code></pre><br><br>")
          }
        }
      }

      var memoryLimit = options.executionMemoryLimit
      var timeLimit = options.executionTimeLimit
      val languageSpecificLimits = options.limits
      val stepikLanguageName = StepikLanguage.langOfId(language.id, languageVersion).langName
      if (languageSpecificLimits != null && stepikLanguageName != null) {
        languageSpecificLimits[stepikLanguageName]?.let {
          memoryLimit = it.memory
          timeLimit = it.time
        }
      }
      if (memoryLimit != null && timeLimit != null) {
        append("""<br><font color="gray">${EduCoreBundle.message("stepik.memory.limit", memoryLimit!!)}</font>""")
        append("""<br><font color="gray">${EduCoreBundle.message("stepik.time.limit", timeLimit!!)}</font><br><br>""")
      }
    }
  }

  private fun codeTask(name: String): CodeTask {
    val task = CodeTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked)
    val options = step.pycharmOptions()

    task.fillDescription()
    initTaskFiles(task, "write your answer here \n", getCodeTemplateForTask(options.codeTemplates))
    return task
  }

  private fun clearCodeBlockFromTags(): String {
    val parsedText = Jsoup.parse(step.text)
    for (element in parsedText.select("code")) {
      val settings = Document.OutputSettings().prettyPrint(false)
      var codeBlockWithoutTags = Jsoup.clean(element.html(), "", Whitelist().addTags("br"), settings)
      codeBlockWithoutTags = codeBlockWithoutTags.replace("<br>", "\n")
      element.html(codeBlockWithoutTags)
    }
    return parsedText.toString()
  }

  private fun choiceTask(name: String): ChoiceTask {
    val task = ChoiceTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked)
    task.descriptionText = clearCodeBlockFromTags()

    val choiceStep: ChoiceStep? = if (courseMode == CCUtils.COURSE_MODE && stepId > 0)
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

    initTaskFiles(task)
    return task
  }

  private fun theoryTask(name: String): TheoryTask {
    val task = TheoryTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked)
    task.descriptionText = clearCodeBlockFromTags()

    initTaskFiles(task)
    return task
  }

  private fun videoTask(name: String): VideoTask {
    val task = VideoTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked)
    var descriptionText = EduCoreBundle.message("stepik.view.video", getStepikLink(task, lesson))
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
    initTaskFiles(task)
    return task
  }

  private fun dataTask(name: String): DataTask {
    val task = DataTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked)
    val options = step.pycharmOptions()

    task.fillDescription()
    initTaskFiles(task, "write your code here \n", getCodeTemplateForTask(options.codeTemplates))

    val isSingleSample = options.samples?.size == 1
    options.samples?.forEachIndexed { index, sample ->
      if (sample.size == 2) {
        val resultIndex = if (isSingleSample) "" else (index + 1).toString()
        val folderName = "$DATA_SAMPLE_FOLDER_NAME$resultIndex"
        val filePath = join(listOf(DATA_FOLDER_NAME, folderName, INPUT_FILE_NAME), VFS_SEPARATOR_CHAR.toString())
        task.addTaskFile(TaskFile(filePath, sample.first()))
      }
      else {
        LOG.warn("Unexpected sample format:")
        sample.forEach {
          LOG.warn("    $it")
        }
      }
    }

    return task
  }

  private fun unsupportedTask(@NonNls name: String): Task {
    val task = TheoryTask(name, stepId, stepSource.position, updateDate, CheckStatus.Unchecked)
    task.descriptionText = "${name.toLowerCase().capitalize()} tasks are not supported yet. <br>" +
                           "View this step on <a href=\"${getStepikLink(task, lesson)}\">Stepik</a>."
    task.postSubmissionOnOpen = false

    initTaskFiles(task, "This is a ${name.toLowerCase()} task. You can use this editor as a playground\n")
    return task
  }

  private fun pycharmTask(): Task {
    val stepOptions = step.pycharmOptions()
    val taskName: String = stepOptions.title ?: DEFAULT_EDU_TASK_NAME

    val task = pluginTaskTypes[stepOptions.taskType]?.invoke(taskName)
               ?: EduTask(taskName, stepId, stepSource.position, updateDate, CheckStatus.Unchecked)
    task.customPresentableName = stepOptions.customPresentableName
    task.solutionHidden = stepOptions.solutionHidden

    task.descriptionText = if (!stepOptions.descriptionText.isNullOrEmpty() && courseType != HYPERSKILL_TYPE) stepOptions.descriptionText.orEmpty() else step.text
    task.descriptionFormat = stepOptions.descriptionFormat

    initTaskFiles(task)
    return task
  }

  private fun initTaskFiles(
    task: Task,
    comment: String = "You can experiment here, it won’t be checked\n",
    codeTemplate: String? = null
  ) {
    val options = step.options
    if (options is PyCharmStepOptions) {
      options.files?.forEach {
        addPlaceholdersTexts(it)
        task.addTaskFile(it)
      }
    }

    if (task.taskFiles.isEmpty()) {
      createMockTaskFile(task, comment, codeTemplate)
    }
  }

  private fun createMockTaskFile(task: Task, comment: String, codeTemplate: String?) {
    val configurator = EduConfiguratorManager.findConfigurator(courseType, courseEnvironment, language)
    if (configurator == null) {
      LOG.error("Could not find configurator for courseType $courseType, language $language")
      return
    }
    val editorText = buildString {
      if (codeTemplate == null) {
        val commentPrefix = LanguageCommenters.INSTANCE.forLanguage(language)?.lineCommentPrefix
        if (commentPrefix != null) {
          append("$commentPrefix $comment")
        }
        append("\n${configurator.mockTemplate}")
      }
      else {
        append(codeTemplate)
      }
    }

    val fileName = configurator.getMockFileName(editorText)
    if (fileName == null) {
      LOG.error(
        "Failed to retrieve fileName: courseType=$courseType, languageId=${language.id}, configurator=${configurator.javaClass.simpleName}")
      return
    }
    val taskFilePath = GeneratorUtils.joinPaths(configurator.sourceDir, fileName)
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
    return StepikLanguage.langOfId(language.id, languageVersion).langName
  }

  companion object {
    private const val DEFAULT_EDU_TASK_NAME = "Edu Task"
    private const val UNKNOWN_TASK_NAME = "Unknown Task"
    private val HTML_TAG_REGEX = "<[^>]+>".toRegex()
    private val LOG = Logger.getInstance(StepikTaskBuilder::class.java)

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

    @VisibleForTesting
    fun isValidHtml(text: String): Boolean = HTML_TAG_REGEX in text
  }
}
