package com.jetbrains.edu.assistant.validation.actions.next.step.hint.code.sequential

import com.jetbrains.edu.assistant.validation.actions.next.step.hint.BaseAssistantInfoStorage
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.util.MultipleCodeHintDataframeRecord
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.educational.ml.hints.core.AIHintsAssistantResponse
import org.apache.commons.csv.CSVRecord
import org.jetbrains.kotlinx.dataframe.api.toDataFrame


/**
 * The `Validate Sequential Code Hint Generation` action prepares a dataset with the hint validation for the set of the solutions.
 * For each solution, this action tries to solve the task over N * 1.5 iterations, where N is the number of generated steps.
 * For each iteration, the number of code quality issues if the generated code is written into the output dataset.
 * The output data can be found in `educational-plugin/aiAssistantValidation/multipleCodeHints.csv` by default, validation output directory can be
 * set up in `gradle.properties`.
 *
 * Example of the output data:
 * ```
 * |  hintIndex |       taskId      |          taskName        |    taskDescription |               taskAnalysisPrompt                             |                |     issues   | testStatus | errorMessage |
 * |:----------:|:-----------------:|:------------------------:|:------------------:|:------------------------------------------------------------:|:--------------:|:---------- -:|:----------:|:------------:|
 * | 0          |     1412191977    |    ProgramEntryPoint     |         ...        | 1. Replace the existing output text with the string "Hello!" |       ...      |      []      |   PASSED   |              |
 * |     ...    |        ...        |           ...            |         ...        |                              ...                             |       ...      |      ...     |   ...      |     ...      |
 * ```
 */
@Suppress("ComponentNotRegistered")
class SequentialCodeHintValidationAction : SequentialCodeValidationAction<MultipleCodeHintDataframeRecord>() {

  override val outputFilePrefixName: String = "multipleCodeHints"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.sequential.code.hint.validation.action.name")
  override val isNavigationRequired: Boolean = true
  override val pathToLabelledDataset = null

  init {
    setUpSpinnerPanel(name)
  }

  override fun CSVRecord.toDataframeRecord() = MultipleCodeHintDataframeRecord.buildFrom(this)

  override fun MutableList<MultipleCodeHintDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun getRecord(
    task: EduTask,
    hintIndex: Int,
    baseAssistantInfoStorage: BaseAssistantInfoStorage,
    response: AIHintsAssistantResponse?,
    userCode: String,
    currentUserCode: String
  ): MultipleCodeHintDataframeRecord {
    val issues = runInspections(baseAssistantInfoStorage.project, baseAssistantInfoStorage.language, currentUserCode)
    return MultipleCodeHintDataframeRecord(
      hintIndex = hintIndex,
      taskId = task.id,
      taskName = task.name,
      taskDescription = baseAssistantInfoStorage.taskProcessor.getTaskTextRepresentation(),
      codeHintPrompt = response?.prompts?.getOrDefault("nextStepCodeHintPrompt", ""),
      userCode = userCode,
      generatedCode = response?.codeHint,
      numberOfIssues = issues.size,
      issues = issues.joinToString(","),
      testStatus = task.status.name,
      errorMessage = task.feedback?.message
    )
  }

  override fun getRecordWhenError(
    task: EduTask,
    hintIndex: Int,
    baseAssistantInfoStorage: BaseAssistantInfoStorage,
    response: AIHintsAssistantResponse?,
    userCode: String,
    e: Throwable
  ) = MultipleCodeHintDataframeRecord(
    hintIndex = hintIndex,
    taskId = task.id,
    taskName = task.name,
    taskDescription = baseAssistantInfoStorage.taskProcessor.getTaskTextRepresentation(),
    userCode = userCode,
    error = e
  )

  override fun needToBreak(task: EduTask) = task.status == CheckStatus.Solved
}
