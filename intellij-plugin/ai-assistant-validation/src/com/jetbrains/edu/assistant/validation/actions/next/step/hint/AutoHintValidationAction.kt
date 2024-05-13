package com.jetbrains.edu.assistant.validation.actions.next.step.hint

import com.google.gson.Gson
import com.intellij.openapi.components.service
import com.jetbrains.edu.assistant.validation.accuracy.AccuracyCalculator
import com.jetbrains.edu.assistant.validation.actions.ValidationAction
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.processor.processValidationHintForItsType
import com.jetbrains.edu.assistant.validation.processor.processValidationHints
import com.jetbrains.edu.assistant.validation.util.*
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import org.apache.commons.csv.CSVRecord
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import kotlin.io.path.Path

/**
 * The `Auto Validate Hints Generation` action runs an automatic validation of educational AI assistant generating text and code hints.
 * The output data can be found in `educational-plugin/aiAssistantValidation/generatedValidationOfHints.csv` by default, validation
 * output directory can be set up in `gradle.properties`.
 *
 * Example of the output data:
 * ```
 * |   taskId   |      taskName     | taskDescription |  solutionSteps  |  userCode  |  nextStepTextHint |  nextStepCodeHint  | errors |  feedbackType  |  information  |  levelOfDetail  |  personalized  |  intersection  |  appropriate  |  specific  |  misleadingInformation  |  codeQuality  | kotlinStyle  |            length               | correlationWithSteps |
 * |:----------:|:-----------------:|:---------------:|:---------------:|:----------:|:-----------------:|:------------------:|:------:|:--------------:|:-------------:|:---------------:|:--------------:|:--------------:|:-------------:|:----------:|:-----------------------:|:-------------:|:------------:|:-------------------------------:|:--------------------:|
 * | 1412191977 | ProgramEntryPoint |       ...       | 1. Start by ... | package... | Replace the ...   | package ...        |   ...  | KTC-TR, ...    |      No       |      BOH        |      Yes       |      No        |     Yes       |    Yes     |           No            |      Yes      |     Yes      |  new: 1, changed: 3, deleted: 0 |       Yes            |
 * |     ...    |        ...        |       ...       |     ...         |     ...    |       ...         |       ...          |   ...  |      ...       |     ...       |       ...       |       ...      |      ...       |     ...       |    ...     |           ...           |      ...      |     ...      |             ...                 |       ...            |
 * ```
 */
@Suppress("ComponentNotRegistered")
class AutoHintValidationAction : ValidationAction<ValidationOfHintsDataframeRecord>() {

  override val outputFilePrefixName: String = "generatedValidationOfHints"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.auto.hint.validation.action.name")
  override val isNavigationRequired: Boolean = true
  override val pathToLabelledDataset by lazy { Path(System.getProperty("manual.hint.validation.path")) }
  override val accuracyCalculator = AutoHintValidationAccuracyCalculator()

  init {
    setUpSpinnerPanel(name)
  }

  override fun MutableList<ValidationOfHintsDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun CSVRecord.toDataframeRecord() = ValidationOfHintsDataframeRecord.buildFrom(this)

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<ValidationOfHintsDataframeRecord> {
    val taskProcessor = TaskProcessor(task)
    val project = task.project ?: error("Cannot get project")
    val eduState = project.eduState ?: error("Cannot get eduState for project ${project.name}")
    val response = project.service<TaskBasedAssistant>().getHint(taskProcessor, eduState)

    try {
      val userCode = eduState.taskFile.getVirtualFile(project)?.getTextFromTaskTextFile() ?: error("Cannot get a user code")
      val taskDescription = taskProcessor.getTaskTextRepresentation()
      val codeHint = response.codeHint ?: error("Cannot get a code hint (${response.assistantError?.name ?: "no assistant error found"})")
      val textHint = response.textHint ?: error("Cannot get a text hint (${response.assistantError?.name ?: "no assistant error found"})")
      val hintType = processValidationHintForItsType(textHint, codeHint)
      val hintsValidation = processValidationHints(taskDescription, textHint, codeHint, userCode)
      val dataframeRecord = Gson().fromJson(hintsValidation, ValidationOfHintsDataframeRecord::class.java)
      return listOf(dataframeRecord.apply {
        taskId = task.id
        taskName = task.name
        this.taskDescription = taskDescription
        this.userCode = userCode
        nextStepTextHint = textHint
        nextStepCodeHint = codeHint
        feedbackType = hintType
      })
    }
    catch (e: Throwable) {
      return listOf(ValidationOfHintsDataframeRecord (
        taskId = task.id,
        taskName = task.name,
        taskDescription = taskProcessor.getTaskTextRepresentation(),
        userCode = eduState.taskFile.getVirtualFile(project)?.getTextFromTaskTextFile() ?: "",
        nextStepCodeHint = response.codeHint ?: "",
        nextStepTextHint = response.textHint ?: "",
        errors = "${EduAndroidAiAssistantValidationBundle.message("action.validation.error")} ${e.message}"
      ))
    }
  }

  override suspend fun buildRecords(manualValidationRecord: ValidationOfHintsDataframeRecord): ValidationOfHintsDataframeRecord {
    try {
      val hintType = processValidationHintForItsType(manualValidationRecord.nextStepTextHint, manualValidationRecord.nextStepCodeHint)
      val hintsValidation = processValidationHints(
        manualValidationRecord.taskDescription,
        manualValidationRecord.nextStepTextHint,
        manualValidationRecord.nextStepCodeHint,
        manualValidationRecord.userCode
      )
      val dataframeRecord = Gson().fromJson(hintsValidation, ValidationOfHintsDataframeRecord::class.java)
      return dataframeRecord.apply {
        taskId = manualValidationRecord.taskId
        taskName = manualValidationRecord.taskName
        this.taskDescription = manualValidationRecord.taskDescription
        this.userCode = manualValidationRecord.userCode
        nextStepTextHint = manualValidationRecord.nextStepTextHint
        nextStepCodeHint = manualValidationRecord.nextStepCodeHint
        feedbackType = hintType
      }
    } catch (e: Throwable) {
      return ValidationOfHintsDataframeRecord (
        taskId = manualValidationRecord.taskId,
        taskName = manualValidationRecord.taskName,
        taskDescription = manualValidationRecord.taskDescription,
      )
    }
  }

  inner class AutoHintValidationAccuracyCalculator : AccuracyCalculator<ValidationOfHintsDataframeRecord> {

    private fun String.getNumberBeforeWord(word: String) = Regex("(\\d+)\\s+$word").find(this)?.groupValues?.get(1)?.toInt()

    private fun areSameLengthCriteria(first: String, second: String): Boolean {
      arrayOf(NEW_KEYWORD, CHANGED_KEYWORD, DELETED_KEYWORD).forEach {
        val firstNumber = first.getNumberBeforeWord(it) ?: return false
        val secondNumber = second.getNumberBeforeWord(it) ?: return false
        if (firstNumber != secondNumber) return false
      }
      return true
    }

    private fun isCorrectLength(answer: String) =
      arrayOf(NEW_KEYWORD, CHANGED_KEYWORD, DELETED_KEYWORD).sumOf { answer.getNumberBeforeWord(it) ?: 0 } < ALLOWABLE_TOTAL_LENGTH_OF_CHANGES

    private fun areSameFeedbackTypeCriteria(first: String, second: String): Boolean {
      val firstList = first.split(",").map { it.trim() }
      val secondList = second.split(",").map { it.trim() }
      return firstList.size == secondList.size && firstList.containsAll(secondList)
    }

    override fun calculateValidationAccuracy(
      manualRecords: List<ValidationOfHintsDataframeRecord>,
      autoRecords: List<ValidationOfHintsDataframeRecord>
    ) = ValidationOfHintsDataframeRecord(
      nextStepCodeHint = ACCURACY_KEYWORD,
      feedbackType = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::feedbackType) { f, s ->
        areSameFeedbackTypeCriteria(f, s!!)
      },
      information = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::information) { f, s ->
        areSameCriteria(f, s!!)
      },
      levelOfDetail = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::levelOfDetail) { f, s ->
        areSameCriteria(f, s!!, BOH_KEYWORD, HLD_KEYWORD)
      },
      personalized = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::personalized) { f, s ->
        areSameCriteria(f, s!!)
      },
      intersection = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::intersection) { f, s ->
        areSameCriteria(f, s!!)
      },
      appropriate = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::appropriate) { f, s ->
        areSameCriteria(f, s!!)
      },
      specific = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::specific) { f, s ->
        areSameCriteria(f, s!!)
      },
      misleadingInformation = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::misleadingInformation) { f, s ->
        areSameCriteria(f, s!!)
      },
      codeQuality = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::codeQuality) { f, s ->
        areSameCriteria(f, s!!)
      },
      kotlinStyle = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::kotlinStyle) { f, s ->
        areSameCriteria(f, s!!)
      },
      length = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfHintsDataframeRecord::length) { f, s ->
        areSameLengthCriteria(f, s!!)
      }
    )

    override fun calculateOverallAccuracy(records: List<ValidationOfHintsDataframeRecord>) = ValidationOfHintsDataframeRecord(
      nextStepCodeHint = ACCURACY_KEYWORD,
      information = calculateCriterionResultAccuracy(records, ValidationOfHintsDataframeRecord::information) { f -> isCorrectAnswer(f) },
      levelOfDetail = calculateCriterionResultAccuracy(records, ValidationOfHintsDataframeRecord::levelOfDetail) { f -> isCorrectAnswer(f, BOH_KEYWORD) },
      personalized = calculateCriterionResultAccuracy(records, ValidationOfHintsDataframeRecord::personalized) { f -> isCorrectAnswer(f) },
      intersection = calculateCriterionResultAccuracy(records, ValidationOfHintsDataframeRecord::intersection) { f -> isCorrectAnswer(f) },
      appropriate = calculateCriterionResultAccuracy(records, ValidationOfHintsDataframeRecord::appropriate) { f -> isCorrectAnswer(f) },
      specific = calculateCriterionResultAccuracy(records, ValidationOfHintsDataframeRecord::specific) { f -> isCorrectAnswer(f) },
      misleadingInformation = calculateCriterionResultAccuracy(records, ValidationOfHintsDataframeRecord::misleadingInformation) { f -> isCorrectAnswer(f, NO_KEYWORD) },
      codeQuality = calculateCriterionResultAccuracy(records, ValidationOfHintsDataframeRecord::codeQuality) { f -> isCorrectAnswer(f) },
      kotlinStyle = calculateCriterionResultAccuracy(records, ValidationOfHintsDataframeRecord::kotlinStyle) { f -> isCorrectAnswer(f) },
      length = calculateCriterionResultAccuracy(records, ValidationOfHintsDataframeRecord::length) { f -> isCorrectLength(f) },
    )
  }
}
