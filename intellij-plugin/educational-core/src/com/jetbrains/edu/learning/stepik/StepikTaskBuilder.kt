package com.jetbrains.edu.learning.stepik

import com.intellij.lang.Language
import com.intellij.lang.LanguageCommenters
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil.join
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.util.text.VersionComparatorUtil
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillTaskType
import com.jetbrains.edu.learning.courseFormat.tasks.*
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATA_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATA_SAMPLE_FOLDER_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.DATA_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask.Companion.INPUT_FILE_NAME
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask.Companion.EDU_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.IdeTask.Companion.IDE_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask.Companion.OUTPUT_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask.Companion.THEORY_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOption
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikBasedConnector.Companion.getStepikBasedConnector
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.xmlEscaped
import com.jetbrains.rd.util.first
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting

open class StepikTaskBuilder(private val course: Course, stepSource: StepSource) {
  private val courseType: String = course.itemType
  private val courseMode: CourseMode = course.courseMode
  private val courseEnvironment: String = course.environment
  private val language: Language = course.languageById ?: Language.ANY
  private val languageVersion: String = course.languageVersion ?: ""
  private val step: Step = stepSource.block ?: error("Step is empty")
  private val stepId: Int = stepSource.id
  private val stepPosition: Int = stepSource.position
  private val updateDate = stepSource.updateDate

  private val pluginTaskTypes: Map<String, (String) -> Task> = mapOf(
    // lexicographical order
    DATA_TASK_TYPE to { name: String -> DataTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    EDU_TASK_TYPE to { name: String -> EduTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    IDE_TASK_TYPE to { name: String -> IdeTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    OUTPUT_TASK_TYPE to { name: String -> OutputTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    REMOTE_EDU_TASK_TYPE to { name: String -> RemoteEduTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
    THEORY_TASK_TYPE to { name: String -> TheoryTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked) },
  )

  private val stepikTaskBuilders: Map<String, (String) -> Task> = HyperskillTaskType.values().associateBy(
    { it.type },
    {
      when (it) {
        // lexicographical order
        HyperskillTaskType.CHOICE -> this::choiceTask
        HyperskillTaskType.CODE -> this::codeTask
        HyperskillTaskType.DATASET -> this::dataTask
        HyperskillTaskType.MATCHING -> this::matchingTask
        HyperskillTaskType.NUMBER -> this::numberTask
        HyperskillTaskType.PYCHARM -> { _: String -> pycharmTask() }
        HyperskillTaskType.REMOTE_EDU -> { _: String -> pycharmTask(REMOTE_EDU_TASK_TYPE) }
        HyperskillTaskType.SORTING -> this::sortingTask
        HyperskillTaskType.STRING -> this::stringTask
        HyperskillTaskType.TABLE -> this::tableTask
        HyperskillTaskType.TEXT -> this::theoryTask
        else -> this::unsupportedTask
      }
    })

  open fun createTask(type: String): Task {
    val taskName = HyperskillTaskType.values().find { it.type == type }?.value ?: UNKNOWN_TASK_NAME
    return (stepikTaskBuilders[type] ?: this::unsupportedTask).invoke(taskName)
  }

  private fun Step.pycharmOptions(): PyCharmStepOptions {
    return options as PyCharmStepOptions
  }

  private fun Task.fillDescription() {
    if (this !is CodeTask && this !is DataTask) return

    val options = step.pycharmOptions()
    val samples = options.samples

    descriptionFormat = DescriptionFormat.HTML
    descriptionText = buildString {
      append(step.text)

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
    val codeTemplates = step.pycharmOptions().codeTemplates
    val (submissionLanguage, codeTemplate) = getLangAndCodeTemplate(codeTemplates.orEmpty())
    val task = CodeTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked, submissionLanguage)

    task.fillDescription()
    initTaskFiles(task, "write your answer here \n", codeTemplate)
    return task
  }

  private fun getLangAndCodeTemplate(codeTemplates: Map<String, String>): Pair<String?, String?> =
    when (codeTemplates.size) {
      0 -> null to null
      1 -> codeTemplates.entries.first().toPair()
      else -> {
        // Select the latest programming language version. We assume that the latest version has backwards compatibility.
        // For example, Java 17 and 11 or Python 3 and 3.10. See https://stepik.org/lesson/63139/step/11 for all available versions.
        // Hyperskill uses more than one version when switches to a new one
        val langWithMaxVersion = codeTemplates.keys
          .mapNotNull { langAndVersionRegex.matchEntire(it)?.groupValues }
          .reduceOrNull { max, curr -> if (VersionComparatorUtil.compare(max[2], curr[2]) > 0) max else curr }
          ?.first()

        if (langWithMaxVersion == null) codeTemplates.first().toPair()
        else langWithMaxVersion to codeTemplates[langWithMaxVersion]
      }
    }

  private fun choiceTask(name: String): ChoiceTask {
    val task = ChoiceTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    task.canCheckLocally = false
    task.descriptionText = step.text
    task.descriptionFormat = DescriptionFormat.HTML

    if (course is EduCourse && courseMode == CourseMode.EDUCATOR && stepId > 0) {
      return task.apply { fillForCourseCreatorMode() }
    }

    if (!isUnitTestMode) {
      when (val result = course.getStepikBasedConnector().getActiveAttemptOrPostNew(task)) {
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

  private fun sortingTask(name: String): SortingTask {
    val task = SortingTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    task.descriptionText = step.text
    task.descriptionFormat = DescriptionFormat.HTML

    if (!isUnitTestMode) {
      when (val result = course.getStepikBasedConnector().getActiveAttemptOrPostNew(task)) {
        is Ok -> fillSortingTask(result.value, task)
        is Err -> LOG.warn("Can't get attempt for Sorting task of $courseType course: ${result.error}")
      }
    }

    initTaskFiles(task)
    return task
  }

  private fun matchingTask(name: String): MatchingTask {
    val task = MatchingTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)

    task.descriptionText = step.text
    task.descriptionFormat = DescriptionFormat.HTML

    if (!isUnitTestMode) {
      when (val result = course.getStepikBasedConnector().getActiveAttemptOrPostNew(task)) {
        is Ok -> fillMatchingTask(result.value, task)
        is Err -> LOG.warn("Can't get attempt for Matching task of $courseType course: ${result.error}")
      }
    }

    initTaskFiles(task)
    return task
  }

  private fun tableTask(name: String): TableTask {
    val task = TableTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)

    task.descriptionText = step.text
    task.descriptionFormat = DescriptionFormat.HTML

    if (!isUnitTestMode) {
      when (val result = course.getStepikBasedConnector().getActiveAttemptOrPostNew(task)) {
        is Ok -> fillTableTask(result.value, task)
        is Err -> LOG.warn("Can't get attempt for Table task of $courseType course: ${result.error}")
      }
    }

    initTaskFiles(task)
    return task
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
    descriptionText = description
    descriptionFormat = DescriptionFormat.HTML

    val text = EduCoreBundle.message("string.task.comment.file")
    val taskFile = TaskFile(AnswerTask.ANSWER_FILE_NAME, text)
    val answerPlaceholder = AnswerPlaceholder(0, text)
    taskFile.addAnswerPlaceholder(answerPlaceholder)
    addTaskFile(taskFile)
  }

  private fun theoryTask(name: String): TheoryTask {
    val task = TheoryTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    task.descriptionText = step.text
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

  // We can get an unsupported task for hyperskill courses only. There only task type is important, no other info is used
  private fun unsupportedTask(@NonNls name: String): Task {
    return UnsupportedTask(name, stepId, stepPosition, updateDate, CheckStatus.Unchecked).apply {
      descriptionFormat = DescriptionFormat.HTML
      initTaskFiles(this)
    }
  }

  private fun pycharmTask(type: String? = null): Task {
    val stepOptions = step.pycharmOptions()
    val taskName: String = stepOptions.title ?: DEFAULT_EDU_TASK_NAME

    val taskType = type ?: stepOptions.taskType
    val task = pluginTaskTypes[taskType]?.invoke(taskName) ?: EduTask(taskName, stepId, stepPosition, updateDate, CheckStatus.Unchecked)
    task.customPresentableName = stepOptions.customPresentableName
    task.solutionHidden = stepOptions.solutionHidden

    task.descriptionText = if (!stepOptions.descriptionText.isNullOrEmpty() && courseType != HYPERSKILL) {
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
    codeTemplate: String? = null,
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

    val fileName = configurator.getMockFileName(course, editorText)
    if (fileName == null) {
      LOG.error(
        "Failed to retrieve fileName: courseType=$courseType, languageId=${language.id}, configurator=${configurator.javaClass.simpleName}")
      return
    }
    val taskFilePath = GeneratorUtils.joinPaths(configurator.sourceDir, fileName)
    val taskFile = TaskFile()
    taskFile.text = editorText
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
    private const val UNKNOWN_TASK_NAME = "Unknown"
    private val LOG = Logger.getInstance(StepikTaskBuilder::class.java)
     val langAndVersionRegex = Regex("^([a-zA-Z+#]+)\\s?([.|0-9]+)\$")

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

    private fun fillSortingTask(attempt: Attempt, task: SortingTask): Boolean {
      val dataset = attempt.dataset
      if (dataset?.options == null) {
        LOG.warn("Dataset for step ${task.id} is null")
        return false
      }
      task.options = dataset.options.orEmpty()
      return true
    }

    private fun fillMatchingTask(attempt: Attempt, task: MatchingTask): Boolean {
      val dataset = attempt.dataset
      if (dataset?.pairs == null) {
        LOG.warn("Dataset for step ${task.id} is null")
        return false
      }
      val pairs = dataset.pairs.orEmpty()
      task.options = pairs.map { it.second }
      task.captions = pairs.map { it.first }
      return true
    }

    private fun fillTableTask(attempt: Attempt, task: TableTask): Boolean {
      val dataset = attempt.dataset
      if (dataset == null) {
        LOG.warn("Dataset for step ${task.id} is null")
        return false
      }
      val rows = dataset.rows
      if (rows == null) {
        LOG.warn("Dataset does not contain any rows for step ${task.id}")
        return false
      }

      val columns = dataset.columns
      if (columns == null) {
        LOG.warn("Dataset does not contain any columns for step ${task.id}")
        return false
      }

      val isCheckbox = dataset.isCheckbox
      task.createTable(rows, columns, isCheckbox)
      return true
    }

    @VisibleForTesting
    fun String.prepareSample(): String = xmlEscaped.replace("\n", "<br>")
  }
}
