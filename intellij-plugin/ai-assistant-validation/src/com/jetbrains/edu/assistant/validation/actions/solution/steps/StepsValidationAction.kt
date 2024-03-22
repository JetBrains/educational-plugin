package com.jetbrains.edu.assistant.validation.actions.solution.steps

import com.jetbrains.edu.assistant.validation.actions.ValidationAction
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.util.StepsDataframeRecord
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import org.apache.commons.csv.CSVRecord
import org.jetbrains.kotlinx.dataframe.api.toDataFrame

/**
 * The `Validate Steps Generation` action prepares a dataset for manual validation of educational AI assistant generating solution steps
 * for the EduTasks.The output data can be found in `educational-plugin/aiAssistantValidation/generatedSteps.csv` by default, validation
 * output directory can be set up in `gradle.properties`.
 *
 * Example of the output data:
 * ```
 * |   taskId   |      taskName     | taskDescription | prompt |                             steps                             |
 * |:----------:|:-----------------:|:---------------:|:------:|:-------------------------------------------------------------:|
 * | 1412191977 | ProgramEntryPoint |       ...       |   ...  | To solve the coding task, follow these steps: ...             |
 * | 1762576790 | BuiltinFunctions  |       ...       |   ...  | 1. Start by declaring a variable called `firstUserAnswer` ... |
 * |     ...    |        ...        |       ...       |   ...  |                              ...                              |
 * ```
 */
@Suppress("ComponentNotRegistered")
class StepsValidationAction : ValidationAction<StepsDataframeRecord>() {

  override val outputFilePrefixName: String = "generatedSteps"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.step.validation.action.name")
  override val isNavigationRequired: Boolean = false
  override val pathToManualValidationDataset = null

  init {
    setUpSpinnerPanel(name)
  }

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<StepsDataframeRecord> {
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)

    try {
      val steps = assistant.getTaskAnalysis(task) ?: ""

      return listOf(StepsDataframeRecord (
        taskId = task.id,
        taskName = task.name,
        taskDescription = taskProcessor.getTaskTextRepresentation(),
        prompt = assistant.taskAnalysisPrompt,
        steps = steps
      ))
    }
    catch (e: Throwable) {
      return listOf(StepsDataframeRecord (
        taskId = task.id,
        taskName = task.name,
        taskDescription = taskProcessor.getTaskTextRepresentation(),
        error = e
      ))
    }
  }

  override fun MutableList<StepsDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun CSVRecord.toDataframeRecord() = StepsDataframeRecord(get(0).toInt(), get(1), get(2), get(3), get(4))

  override fun calculateAccuracy(manualRecords: List<StepsDataframeRecord>, autoRecords: List<StepsDataframeRecord>): StepsDataframeRecord {
    throw UnsupportedOperationException("This function is not supported.")
  }

  override suspend fun buildRecords(manualValidationRecord: StepsDataframeRecord): StepsDataframeRecord {
    throw UnsupportedOperationException("This function is not supported.")
  }
}
