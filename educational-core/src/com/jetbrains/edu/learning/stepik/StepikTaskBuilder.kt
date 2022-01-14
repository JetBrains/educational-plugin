package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.lang.Language
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil.join
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask.Companion.CODE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.Companion.EDU_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.Companion.PYCHARM_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask.Companion.IDE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.NumberTask.Companion.NUMBER_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask.Companion.OUTPUT_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.StringTask.Companion.STRING_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask.Companion.THEORY_TASK_TYPE
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
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.Attempt
import com.jetbrains.edu.learning.stepik.api.StepikBaseConnector.Companion.getConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.VideoTaskResourcesManager
import com.jetbrains.edu.learning.xmlEscaped
import org.jetbrains.annotations.NonNls
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist
import java.util.*
import java.util.Collections.unmodifiableList

open class StepikTaskBuilder(private val course: Course, private val lesson: Lesson, stepSource: StepSource) {
  private val courseType: String = course.itemType
  private val courseMode: String = course.courseMode
  private val courseEnvironment: String = course.environment
  private val language: Language = course.languageById ?: Language.ANY
  private val languageVersion: String = course.languageVersion ?: ""
  private val step: Step = stepSource.block ?: error("Step is empty")
  private val stepId: Int = stepSource.id
  private val stepPosition: Int = stepSource.position
  private val updateDate = stepSource.updateDate ?: Date(0)

  private val pluginTaskTypes: Map<String, (String) -> Task> = mapOf(
    // lexicographical order
    DATA_TASK_TYPE to { name: String -> DataTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    EDU_TASK_TYPE to { name: String -> EduTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    IDE_TASK_TYPE to { name: String -> IdeTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    OUTPUT_TASK_TYPE to { name: String -> OutputTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    REMOTE_EDU_TASK_TYPE to { name: String -> RemoteEduTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    THEORY_TASK_TYPE to { name: String -> TheoryTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
  )

  private val stepikTaskBuilders: Map<String, (String) -> Task> = StepikTaskType.values().associateBy(
    { it.type },
    {
      when (it) {
        // lexicographical order
        StepikTaskType.CHOICE -> this::choiceTask
        StepikTaskType.CODE -> this::codeTask
        StepikTaskType.DATASET -> this::dataTask
        StepikTaskType.NUMBER -> this::numberTask
        StepikTaskType.PYCHARM -> { _: String -> pycharmTask() }
        StepikTaskType.REMOTE_EDU -> { _: String -> pycharmTask(REMOTE_EDU_TASK_TYPE) }
        StepikTaskType.STRING -> this::stringTask
        StepikTaskType.TEXT -> this::theoryTask
        StepikTaskType.VIDEO -> this::videoTask
        else -> this::unsupportedTask
      }
    })

  // lexicographical order
  enum class StepikTaskType(val type: String, val value: String) {
    ADMIN("admin", "Linux"),
    CHOICE(CHOICE_TASK_TYPE, "Quiz"),
    CODE(CODE_TASK_TYPE, "Programming"),
    DATASET(DATA_TASK_TYPE, "Data"),
    FREE_ANSWER("free-answer", "Free Response"),
    MANUAL_SCORE("manual-score", "Manual Score"),
    MATCHING("matching", "Matching"),
    MATH("math", "Math"),
    NUMBER(NUMBER_TASK_TYPE, "Number"),
    PYCHARM(PYCHARM_TASK_TYPE, "Programming"),
    REMOTE_EDU(REMOTE_EDU_TASK_TYPE, "Programming"),
    SORTING("sorting", "Sorting"),
    STRING(STRING_TASK_TYPE, "Text"),
    TABLE("table", "Table"),
    TEXT("text", "Theory"),
    VIDEO(VIDEO_TASK_TYPE, "Video")
  }

  open fun createTask(type: String): Task? {
    val taskName = StepikTaskType.values().find { it.type == type }?.value ?: UNKNOWN_TASK_NAME
    return stepikTaskBuilders[type]?.invoke(taskName)
  }

  fun isSupported(type: String): Boolean = stepikTaskBuilders.containsKey(type)

  private fun Step.pycharmOptions(): PyCharmStepOptions {
    return options as PyCharmStepOptions
  }

  private fun Task.fillDescription() {
    if (this !is CodeTask && this !is DataTask) return

    val options = step.pycharmOptions()
    val samples = options.samples

    descriptionFormat = DescriptionFormat.HTML
    descriptionText = buildString {
      append(clearCodeBlockFromTags(step))

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
    val task = CodeTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    val options = step.pycharmOptions()

    task.fillDescription()
    initTaskFiles(task, "write your answer here \n", getCodeTemplateForTask(options.codeTemplates))
    return task
  }

  private fun choiceTask(name: String): ChoiceTask {
    val task = ChoiceTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    task.descriptionText = clearCodeBlockFromTags(step)
    task.descriptionFormat = DescriptionFormat.HTML

    if (course is EduCourse && courseMode == CCUtils.COURSE_MODE && stepId > 0) {
      return task.apply { fillForCourseCreatorMode() }
    }

    if (!isUnitTestMode) {
      when (val result = course.getConnector().getActiveAttemptOrPostNew(task)) {
        is Ok -> fillChoiceTask(result.value, task)
        is Err -> LOG.warn("Can't get attempt for Choice task of $courseType course: ${result.error}")
      }
    }

    initTaskFiles(task)
    return task
  }

  private fun ChoiceTask.fillForCourseCreatorMode() {
    val choiceStep = StepikConnector.getInstance().getChoiceStepSource(stepId)
    if (choiceStep != null) {
      choiceStep.source?.let { choiceStepOptions ->
        isMultipleChoice = choiceStepOptions.isMultipleChoice
        choiceOptions = choiceStepOptions.options.map { ChoiceOption(it.text, it.choiceStatus) }
      }
      if (choiceStep.feedbackCorrect.isNotEmpty()) {
        messageCorrect = choiceStep.feedbackCorrect
      }
      if (choiceStep.feedbackWrong.isNotEmpty()) {
        messageIncorrect = choiceStep.feedbackWrong
      }
    }
    initTaskFiles(this)
  }

  private fun stringTask(name: String): StringTask {
    val stringTask = StringTask(name, stepId, stepPosition, updateDate)
    stringTask.init(step.text)
    return stringTask
  }

  private fun numberTask(name: String): NumberTask {
    val numberTask = NumberTask(name, stepId, stepPosition, updateDate)
    numberTask.init(step.text)
    return numberTask
  }

  private fun AnswerTask.init(description: String) {
    descriptionText = clearCodeBlockFromTags(description)
    descriptionFormat = DescriptionFormat.HTML

    val text = EduCoreBundle.message("string.task.comment.file")
    val taskFile = TaskFile(AnswerTask.ANSWER_FILE_NAME, text)
    val answerPlaceholder = AnswerPlaceholder(0, text)
    taskFile.addAnswerPlaceholder(answerPlaceholder)
    addTaskFile(taskFile)
  }

  private fun theoryTask(name: String): TheoryTask {
    val task = TheoryTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    task.descriptionText = clearCodeBlockFromTags(step)
    task.descriptionFormat = DescriptionFormat.HTML

    initTaskFiles(task)
    return task
  }

  private fun videoTask(name: String): VideoTask {
    val task = VideoTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    var descriptionText = EduCoreBundle.message("stepik.view.video", getStepikLink(task, lesson))
    val video = step.video
    if (video != null) {
      task.thumbnail = video.thumbnail
      task.sources = unmodifiableList(video.listUrls?.map { VideoSource(it.url, it.quality) } ?: emptyList())
      descriptionText = VideoTaskResourcesManager().getText(task, lesson)
    }
    else {
      LOG.warn("Video for step $stepId is null")
    }

    task.descriptionText = descriptionText
    task.descriptionFormat = DescriptionFormat.HTML
    initTaskFiles(task)
    return task
  }

  private fun dataTask(name: String): DataTask {
    val task = DataTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
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
    val task = TheoryTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    task.descriptionText = "${name.toLowerCase().capitalize()} tasks are not supported yet. <br>" +
                           "View this step on <a href=\"${getStepikLink(task, lesson)}\">Stepik</a>."
    task.descriptionFormat = DescriptionFormat.HTML
    task.postSubmissionOnOpen = false

    initTaskFiles(task, "This is a ${name.toLowerCase()} task. You can use this editor as a playground\n")
    return task
  }

  private fun pycharmTask(type: String? = null): Task {
    val stepOptions = step.pycharmOptions()
    val taskName: String = stepOptions.title ?: DEFAULT_EDU_TASK_NAME

    val taskType = type ?: stepOptions.taskType
    val task = pluginTaskTypes[taskType]?.invoke(taskName) ?: EduTask(taskName, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    task.customPresentableName = stepOptions.customPresentableName
    task.solutionHidden = stepOptions.solutionHidden

    task.descriptionText = if (!stepOptions.descriptionText.isNullOrEmpty() && courseType != HYPERSKILL_TYPE) {
      stepOptions.descriptionText.orEmpty()
    }
    else {
      step.text
    }
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

    fun fillChoiceTask(attempt: Attempt, task: ChoiceTask): Boolean {
      val dataset = attempt.dataset
      if (dataset?.options == null) {
        LOG.warn("Dataset for step ${task.id} is null")
        return false
      }
      task.choiceOptions = dataset.options.orEmpty().map(::ChoiceOption)
      task.isMultipleChoice = dataset.isMultipleChoice
      return true
    }

    private fun clearCodeBlockFromTags(text: String): String {
      val parsedText = Jsoup.parse(text)
      for (element in parsedText.select("code")) {
        val settings = Document.OutputSettings().prettyPrint(false)
        var codeBlockWithoutTags = Jsoup.clean(element.html(), "", Whitelist().addTags("br"), settings)
        codeBlockWithoutTags = codeBlockWithoutTags.replace("<br>", "\n")
        element.html(codeBlockWithoutTags)
      }
      return parsedText.toString()
    }

    private fun clearCodeBlockFromTags(step: Step): String {
      return clearCodeBlockFromTags(step.text)
    }

    @VisibleForTesting
    fun String.prepareSample(): String = xmlEscaped.replace("\n", "<br>")
  }
}
