package com.jetbrains.edu.assistant.validation.actions.next.step.hint

import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.psi.PsiFileFactory
import com.jetbrains.edu.assistant.validation.accuracy.AccuracyCalculator
import com.jetbrains.edu.assistant.validation.actions.ValidationAction
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.util.CodeHintDataframeRecord
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getText
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.inspection.InspectionProvider
import com.jetbrains.edu.learning.eduAssistant.inspection.getInspectionsWithIssues
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import org.apache.commons.csv.CSVRecord
import org.jetbrains.kotlinx.dataframe.api.toDataFrame

/**
 * The `Validate Code Hint Generation` action prepares a dataset for manual validation of educational AI assistant generating code hints.
 * The output data can be found in `educational-plugin/aiAssistantValidation/codeHints.csv` by default, validation output directory can be
 * set up in `gradle.properties`.
 *
 * Example of the output data:
 * ```
 * |   taskId   |      taskName     | taskDescription | taskAnalysisPrompt |                             steps                            | codeHintPrompt |            userCode            | errors |    generatedCode    | numberOfIssues | issues |
 * |:----------:|:-----------------:|:---------------:|:------------------:|:------------------------------------------------------------:|:--------------:|:------------------------------:|:------:|:-------------------:|:--------------:|:------:|
 * | 1412191977 | ProgramEntryPoint |       ...       |         ...        | 1. Replace the existing output text with the string "Hello!" |       ...      | println(""My first program!"") |   ...  | println(""Hello!"") |        0       |        |
 * |     ...    |        ...        |       ...       |         ...        |                              ...                             |       ...      |               ...              |   ...  |         ...         |       ...      |   ...  |
 * ```
 */
@Suppress("ComponentNotRegistered")
class CodeHintValidationAction : ValidationAction<CodeHintDataframeRecord>() {

  override val outputFilePrefixName: String = "codeHints"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.code.hint.validation.action.name")
  override val isNavigationRequired: Boolean = true
  override val toCalculateOverallAccuracy: Boolean = false
  override val pathToLabelledDataset = null
  override val accuracyCalculator = CodeHintAccuracyCalculator()

  init {
    setUpSpinnerPanel(name)
  }

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<CodeHintDataframeRecord> {
    return getCodeFromTaskFiles(task, lesson).map {
      runBlockingCancellable {
        buildCodeHintRecord(task, it)
      }
    }
  }

  private suspend fun buildCodeHintRecord(task: EduTask, userCode: String): CodeHintDataframeRecord {
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    val language = task.course.languageById ?: error("Language could not be determined")
    val project = task.project ?: error("Cannot get project")
    val eduState = project.eduState ?: error("Cannot get eduState for project ${project.name}")
    val taskRepresentation = taskProcessor.getTaskTextRepresentation()

    val response = assistant.getHint(task, eduState, userCode)

    try {
      if (response.assistantError != null) error("Assistant error: ${response.assistantError?.name}")
      val psiFile = PsiFileFactory.getInstance(project).createFileFromText("file", language, userCode)
      val inspections = InspectionProvider.getInspections(language)
      val issues = psiFile.getInspectionsWithIssues(inspections).map { it.id }

      return CodeHintDataframeRecord(
        taskId = task.id,
        taskName = task.name,
        taskDescription = taskRepresentation,
        taskAnalysisPrompt = assistant.taskAnalysisPrompt,
        steps = task.generatedSolutionSteps,
        codeHintPrompt = response.prompts.getOrDefault("nextStepCodeHintPrompt", ""),
        userCode = userCode,
        generatedCode = response.codeHint ?: "",
        numberOfIssues = issues.size,
        issues = issues.joinToString(",")
      )
    }
    catch (e: Throwable) {
      return CodeHintDataframeRecord(
        taskId = task.id,
        taskName = task.name,
        taskDescription = taskRepresentation,
        taskAnalysisPrompt = assistant.taskAnalysisPrompt,
        steps = task.generatedSolutionSteps,
        codeHintPrompt = response.prompts.getOrDefault("nextStepCodeHintPrompt", ""),
        userCode = userCode,
        error = e
      )
    }
  }

  private fun checkTaskFile(taskFile: TaskFile) = (taskFile.isBinary == false) && taskFile.isVisible

  private fun getCodeFromTaskFiles(task: EduTask, lesson: Lesson): List<String> {
    val project = task.project ?: error("Cannot get project")
    if (lesson is FrameworkLesson) {
      val state: Map<String, String> = FrameworkLessonManager.getInstance(project).getTaskState(lesson, task)
      return state.filterKeys { task.taskFiles[it] != null && checkTaskFile(task.taskFiles[it]!!) }.values.toList()
    }
    return task.taskFiles.values.filter { checkTaskFile(it) }.mapNotNull { it.getText(project) }
  }

  override fun MutableList<CodeHintDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun CSVRecord.toDataframeRecord() = CodeHintDataframeRecord(get(0).toInt(), get(1), get(2), get(3), get(4), get(5), get(6),
    get(7), get(8), get(9).toInt(), get(10))

  override suspend fun buildRecords(manualValidationRecord: CodeHintDataframeRecord): CodeHintDataframeRecord {
    throw UnsupportedOperationException("This function is not supported.")
  }

  inner class CodeHintAccuracyCalculator : AccuracyCalculator<CodeHintDataframeRecord>() {
    override fun calculateValidationAccuracy(
      manualRecords: List<CodeHintDataframeRecord>,
      autoRecords: List<CodeHintDataframeRecord>
    ): CodeHintDataframeRecord {
      throw UnsupportedOperationException("This function is not supported.")
    }

    override fun calculateOverallAccuracy(records: List<CodeHintDataframeRecord>): CodeHintDataframeRecord {
      throw UnsupportedOperationException("This function is not supported.")
    }
  }
}
