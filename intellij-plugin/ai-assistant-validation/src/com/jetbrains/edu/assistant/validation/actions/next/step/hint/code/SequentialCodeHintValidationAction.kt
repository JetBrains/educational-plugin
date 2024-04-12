package com.jetbrains.edu.assistant.validation.actions.next.step.hint.code

import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.EDT
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.jetbrains.edu.assistant.validation.actions.next.step.hint.BaseAssistantInfoStorage
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.util.MultipleCodeHintDataframeRecord
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.check.EduAssistantValidationCheckListener
import com.jetbrains.edu.learning.eduAssistant.core.AssistantResponse
import com.jetbrains.edu.learning.progress.withBackgroundProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
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
class SequentialCodeHintValidationAction : CodeValidationAction<MultipleCodeHintDataframeRecord>() {

  override val outputFilePrefixName: String = "multipleCodeHints"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.sequential.code.hint.validation.action.name")
  override val isNavigationRequired: Boolean = true
  override val pathToLabelledDataset = null

  init {
    setUpSpinnerPanel(name)
  }

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<MultipleCodeHintDataframeRecord> {
    return getCodeFromTaskFiles(task, lesson).map {
      runBlockingCancellable {
        buildCodeHintRecords(task, it, lesson)
      }
    }.flatten()
  }

  override fun CSVRecord.toDataframeRecord() = MultipleCodeHintDataframeRecord.buildFrom(this)

  override fun MutableList<MultipleCodeHintDataframeRecord>.convertToDataFrame() = toDataFrame()

  private fun Task.resetStatus() {
    status = CheckStatus.Unchecked
  }

  private suspend fun waitForProjectConfiguration(project: Project) {
    while (DumbService.isDumb(project)) {
      delay(50)
    }
  }

  private suspend fun buildCodeHintRecords(
    task: EduTask,
    userCode: String,
    lesson: Lesson,
  ): List<MultipleCodeHintDataframeRecord> {
    val baseAssistantInfoStorage = BaseAssistantInfoStorage(task)
    waitForProjectConfiguration(baseAssistantInfoStorage.project)

    // To avoid saving old task states
    task.resetStatus()

    val records = mutableListOf<MultipleCodeHintDataframeRecord>()

    baseAssistantInfoStorage.assistant.getTaskAnalysis(task)
    task.generatedSolutionSteps ?: error("Cannot get generate solution steps for task ${task.name}")
    var currentUserCode = userCode
    val maxHintSteps = (with(baseAssistantInfoStorage.assistant) {
      task.generatedSolutionSteps?.parseSteps()?.size ?: 0
    } * 1.5).toInt()

    for (hintIndex in 1..maxHintSteps) {
      var response: AssistantResponse? = null
      try {
        // TODO: cannot replace with runBlockingCancellable because of deadlocks
        runBlocking {
          withBackgroundProgress(baseAssistantInfoStorage.project, GETTING_HINT_MESSAGE, false) {
            response = baseAssistantInfoStorage.assistant.getHint(task, baseAssistantInfoStorage.eduState, currentUserCode)
          }
        }

        currentUserCode = response?.codeHint ?: error("Code hint is empty")

        lesson.replaceContent(task, currentUserCode, baseAssistantInfoStorage.eduState, baseAssistantInfoStorage.project)
        val issues = runInspections(baseAssistantInfoStorage.project, baseAssistantInfoStorage.language, currentUserCode)

        val checkListener = CheckListener.EP_NAME.findExtension(EduAssistantValidationCheckListener::class.java)
                            ?: error("Check listener not found")
        checkListener.clear()

        withContext(Dispatchers.EDT) {
          val dataContext = SimpleDataContext.getProjectContext(baseAssistantInfoStorage.project)
          ActionUtil.invokeAction(CheckAction(), dataContext, "", null, null)
        }

        checkListener.wait()

        records.add(
          MultipleCodeHintDataframeRecord(
            hintIndex = hintIndex,
            taskId = task.id,
            taskName = task.name,
            taskDescription = baseAssistantInfoStorage.taskProcessor.getTaskTextRepresentation(),
            taskAnalysisPrompt = baseAssistantInfoStorage.assistant.taskAnalysisPrompt,
            steps = task.generatedSolutionSteps,
            codeHintPrompt = response?.prompts?.getOrDefault("nextStepCodeHintPrompt", ""),
            userCode = userCode,
            generatedCode = response?.codeHint,
            numberOfIssues = issues.size,
            issues = issues.joinToString(","),
            testStatus = task.status.name,
            errorMessage = task.feedback?.message
          )
        )

        if (task.status == CheckStatus.Solved) {
          break
        }
      }
      catch (e: Throwable) {
        records.add(
          MultipleCodeHintDataframeRecord(
            hintIndex = hintIndex,
            taskId = task.id,
            taskName = task.name,
            taskDescription = baseAssistantInfoStorage.taskProcessor.getTaskTextRepresentation(),
            userCode = userCode,
            error = e
          )
        )
      }
    }

    lesson.replaceContent(task, currentUserCode, baseAssistantInfoStorage.eduState, baseAssistantInfoStorage.project)
    return records
  }

  companion object {
    private const val GETTING_HINT_MESSAGE = "Getting Hint"
  }
}
