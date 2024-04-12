package com.jetbrains.edu.assistant.validation.actions.solution.steps

import com.google.gson.Gson
import com.jetbrains.edu.assistant.validation.accuracy.AccuracyCalculator
import com.jetbrains.edu.assistant.validation.actions.ValidationAction
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.processor.processValidationSteps
import com.jetbrains.edu.assistant.validation.util.*
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.project
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
 * |   taskId   |      taskName     | taskDescription | errors |              steps                |  solution |  amount  |  specifics  |  independence  |  codingSpecific  |  direction  |  misleadingInformation  |  granularity  |  kotlinStyle  |
 * |:----------:|:-----------------:|:---------------:|:------:|:---------------------------------:|:---------:|:--------:|:-----------:|:--------------:|:----------------:|:-----------:|:-----------------------:|:-------------:|:-------------:|
 * | 1412191977 | ProgramEntryPoint |       ...       |   ...  | 1. Start by declaring ... 2. ...  |    ...    |    3     |    Yes      |     Yes        |     Coding       |     Yes     |          No             |     Yes       |     Yes       |
 * | 1762576790 | BuiltinFunctions  |       ...       |   ...  |              ...                  |    ...    |    5     |    Yes      |     No         |     No coding    |     No      |          Yes            |     No        |     No        |
 * |     ...    |        ...        |       ...       |   ...  |              ...                  |    ...    |   ...    |    ...      |     ...        |       ...        |     ...     |          ...            |     ...       |     ...       |
 * ```
 */
@Suppress("ComponentNotRegistered")
class AutoStepsValidationAction : ValidationAction<ValidationOfStepsDataframeRecord>() {

  override val outputFilePrefixName: String = "generatedValidationOfSteps"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.auto.step.validation.action.name")
  override val isNavigationRequired: Boolean = false
  override val toCalculateOverallAccuracy: Boolean = true
  override val pathToLabelledDataset by lazy { Path(System.getProperty("manual.steps.validation.path")) }
  override val accuracyCalculator = AutoStepsValidationAccuracyCalculator()

  init {
    setUpSpinnerPanel(name)
  }

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<ValidationOfStepsDataframeRecord> {
    val project = task.project ?: error("Cannot get project")
    val authorSolution = getAuthorSolution(task, project)
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    var stepsValidation: String? = null
    val currentTaskDescription = taskProcessor.getTaskTextRepresentation()

    try {
      val currentSteps = assistant.getTaskAnalysis(task) ?: error("Error during validation generation: code is not compilable")
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
        taskDescription = currentTaskDescription,
        errors = "${EduAndroidAiAssistantValidationBundle.message("action.validation.error")} ${e.message}",
        solution = stepsValidation ?: ""
      ))
    }
  }

  override fun MutableList<ValidationOfStepsDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun CSVRecord.toDataframeRecord() = ValidationOfStepsDataframeRecord.buildFrom(this)

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

  inner class AutoStepsValidationAccuracyCalculator : AccuracyCalculator<ValidationOfStepsDataframeRecord>() {
    override fun calculateValidationAccuracy(
      manualRecords: List<ValidationOfStepsDataframeRecord>,
      autoRecords: List<ValidationOfStepsDataframeRecord>
    ) = ValidationOfStepsDataframeRecord(
      solution = ACCURACY_KEYWORD,
      amount = calculateCriterionAccuracy(manualRecords, autoRecords, { this.amount.toString() }) { f, s ->
        f == s
      }.toDouble().toInt(),
      specifics = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfStepsDataframeRecord::specifics) { f, s ->
        areSameCriteria(f, s!!)
      },
      independence = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfStepsDataframeRecord::independence) { f, s ->
        areSameCriteria(f, s!!)
      },
      codingSpecific = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfStepsDataframeRecord::codingSpecific) { f, s ->
        areSameCriteria(f, s!!, CODING_KEYWORD, NOT_CODING_KEYWORD)
      },
      direction = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfStepsDataframeRecord::direction) { f, s ->
        areSameCriteria(f, s!!)
      },
      misleadingInformation = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfStepsDataframeRecord::misleadingInformation) { f, s ->
        areSameCriteria(f, s!!)
      },
      granularity = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfStepsDataframeRecord::granularity) { f, s ->
        areSameCriteria(f, s!!)
      },
      kotlinStyle = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfStepsDataframeRecord::kotlinStyle) { f, s ->
        areSameCriteria(f, s!!)
      }
    )

    override fun calculateOverallAccuracy(records: List<ValidationOfStepsDataframeRecord>) = ValidationOfStepsDataframeRecord(
      solution = ACCURACY_KEYWORD,
      specifics = calculateCriterionResultAccuracy(records, ValidationOfStepsDataframeRecord::specifics) { f -> isCorrectAnswer(f) },
      independence = calculateCriterionResultAccuracy(records, ValidationOfStepsDataframeRecord::independence) { f -> isCorrectAnswer(f) },
      codingSpecific = calculateCriterionResultAccuracy(records, ValidationOfStepsDataframeRecord::codingSpecific) { f -> isCorrectAnswer(f, CODING_KEYWORD) },
      direction = calculateCriterionResultAccuracy(records, ValidationOfStepsDataframeRecord::direction) { f -> isCorrectAnswer(f) },
      misleadingInformation = calculateCriterionResultAccuracy(records, ValidationOfStepsDataframeRecord::misleadingInformation) { f -> isCorrectAnswer(f, NO_KEYWORD) },
      granularity = calculateCriterionResultAccuracy(records, ValidationOfStepsDataframeRecord::granularity) { f -> isCorrectAnswer(f) },
      kotlinStyle = calculateCriterionResultAccuracy(records, ValidationOfStepsDataframeRecord::kotlinStyle) { f -> isCorrectAnswer(f) },
    )
  }
}
