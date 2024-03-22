package com.jetbrains.edu.assistant.validation.actions.solution.steps

import com.google.gson.Gson
import com.jetbrains.edu.assistant.validation.actions.ValidationAction
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.processor.processValidationSteps
import com.jetbrains.edu.assistant.validation.util.*
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import org.apache.commons.csv.CSVRecord
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import kotlin.io.path.Path

/**
 * The `Auto Validate Steps Generation` action runs an automatic validation of educational AI assistant generating solution steps.
 * The output data can be found in `educational-plugin/aiAssistantValidation/generatedValidationOfSteps.csv` by default, validation
 * output directory can be set up in `gradle.properties`.
 *
 * Example of the output data:
 * ```
 * |   taskId   |      taskName     | taskDescription |              steps                |  solution |  amount  |  specifics  |  independence  |  codingSpecific  |  direction  |  misleadingInformation  |  granularity  |  kotlinStyle  |
 * |:----------:|:-----------------:|:---------------:|:---------------------------------:|:---------:|:--------:|:-----------:|:--------------:|:----------------:|:-----------:|:-----------------------:|:-------------:|:-------------:|
 * | 1412191977 | ProgramEntryPoint |       ...       | 1. Start by declaring ... 2. ...  |    ...    |    3     |    Yes      |     Yes        |     Coding       |     Yes     |          No             |     Yes       |     Yes       |
 * | 1762576790 | BuiltinFunctions  |       ...       |              ...                  |    ...    |    5     |    Yes      |     No         |     No coding    |     No      |          Yes            |     No        |     No        |
 * |     ...    |        ...        |       ...       |              ...                  |    ...    |   ...    |    ...      |     ...        |       ...        |     ...     |          ...            |     ...       |     ...       |
 * ```
 */
@Suppress("ComponentNotRegistered")
class AutoStepsValidationAction : ValidationAction<ValidationOfStepsDataframeRecord>() {

  override val outputFilePrefixName: String = "generatedValidationOfSteps"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.auto.step.validation.action.name")
  override val isNavigationRequired: Boolean = false
  override val pathToManualValidationDataset by lazy { Path(System.getProperty("manual.steps.validation.path")) }

  init {
    setUpSpinnerPanel(name)
  }

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<ValidationOfStepsDataframeRecord> {
    val project = task.project ?: error("Cannot get project")
    val authorSolution = getAuthorSolution(task, project)
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    var stepsValidation: String? = null

    try {
      val currentSteps = assistant.getTaskAnalysis(task) ?: ""
      val currentTaskDescription = taskProcessor.getTaskTextRepresentation()
      stepsValidation = processValidationSteps(currentTaskDescription, authorSolution, currentSteps)
      val dataframeRecord = Gson().fromJson(stepsValidation, ValidationOfStepsDataframeRecord::class.java)
      return listOf(dataframeRecord.apply {
        taskId = task.id
        taskName = task.name
        taskDescription = currentTaskDescription
        steps = currentSteps
        solution = authorSolution
      })
    } catch (e: Throwable) {
      return listOf(ValidationOfStepsDataframeRecord (
        taskId = task.id,
        taskName = task.name,
        taskDescription = taskProcessor.getTaskTextRepresentation(),
        steps = "${EduAndroidAiAssistantValidationBundle.message("action.validation.error")} ${e.message}",
        solution = stepsValidation ?: ""
      ))
    }
  }

  override fun MutableList<ValidationOfStepsDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun CSVRecord.toDataframeRecord() = ValidationOfStepsDataframeRecord(get(0).toInt(), get(1), get(2), get(3), get(4),
    get(5).toInt(), get(6), get(7), get(8), get(9), get(10), get(11), get(12))

  override suspend fun buildRecords(manualValidationRecord: ValidationOfStepsDataframeRecord): ValidationOfStepsDataframeRecord {
    try {
      val stepsValidation = processValidationSteps(manualValidationRecord.taskDescription, manualValidationRecord.solution, manualValidationRecord.steps)
      val dataframeRecord = Gson().fromJson(stepsValidation, ValidationOfStepsDataframeRecord::class.java)
      return dataframeRecord.apply {
        taskId = manualValidationRecord.taskId
        taskName = manualValidationRecord.taskName
        taskDescription = manualValidationRecord.taskDescription
        steps = manualValidationRecord.steps
        solution = manualValidationRecord.solution
      }
    } catch (e: Throwable) {
      return ValidationOfStepsDataframeRecord(
        taskId = manualValidationRecord.taskId,
        taskName = manualValidationRecord.taskName,
        taskDescription = manualValidationRecord.taskDescription,
        steps = "${EduAndroidAiAssistantValidationBundle.message("action.validation.error")} ${e.message}"
      )
    }
  }

  override fun calculateAccuracy(
    manualRecords: List<ValidationOfStepsDataframeRecord>,
    autoRecords: List<ValidationOfStepsDataframeRecord>
  ) = ValidationOfStepsDataframeRecord(
    solution = ACCURACY_KEYWORD,
    amount = calculateCriterionAccuracy(
      { i -> manualRecords[i].amount.toString() },
      { i -> autoRecords[i].amount.toString() },
      manualRecords.size,
      { f, s -> f == s }
    ).toDouble().toInt(),
    specifics = calculateCriterionAccuracy(
      { i -> manualRecords[i].specifics },
      { i -> autoRecords[i].specifics },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    independence = calculateCriterionAccuracy(
      { i -> manualRecords[i].independence },
      { i -> autoRecords[i].independence },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    codingSpecific = calculateCriterionAccuracy(
      { i -> manualRecords[i].codingSpecific },
      { i -> autoRecords[i].codingSpecific },
      manualRecords.size,
      { f, s -> compareCriterion(f, s, CODING_KEYWORD, NOT_CODING_KEYWORD) }
    ),
    direction = calculateCriterionAccuracy(
      { i -> manualRecords[i].direction },
      { i -> autoRecords[i].direction },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    misleadingInformation = calculateCriterionAccuracy(
      { i -> manualRecords[i].misleadingInformation },
      { i -> autoRecords[i].misleadingInformation },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    granularity = calculateCriterionAccuracy(
      { i -> manualRecords[i].granularity },
      { i -> autoRecords[i].granularity },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    kotlinStyle = calculateCriterionAccuracy(
      { i -> manualRecords[i].kotlinStyle },
      { i -> autoRecords[i].kotlinStyle },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    )
  )
}
