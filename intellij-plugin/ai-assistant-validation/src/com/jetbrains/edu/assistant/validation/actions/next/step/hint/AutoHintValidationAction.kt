package com.jetbrains.edu.assistant.validation.actions.next.step.hint

import com.google.gson.Gson
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
 * |   taskId   |      taskName     | taskDescription |  solutionSteps  |  userCode  |  nextStepTextHint |  nextStepCodeHint  |  feedbackType  |  information  |  levelOfDetail  |  personalized  |  intersection  |  appropriate  |  specific  |  misleadingInformation  |  codeQuality  | kotlinStyle  |            length               | correlationWithSteps |
 * |:----------:|:-----------------:|:---------------:|:---------------:|:----------:|:-----------------:|:------------------:|:--------------:|:-------------:|:---------------:|:--------------:|:--------------:|:-------------:|:----------:|:-----------------------:|:-------------:|:------------:|:-------------------------------:|:--------------------:|
 * | 1412191977 | ProgramEntryPoint |       ...       | 1. Start by ... | package... | Replace the ...   | package ...        | KTC-TR, ...    |      No       |      BOH        |      Yes       |      No        |     Yes       |    Yes     |           No            |      Yes      |     Yes      |  new: 1, changed: 3, deleted: 0 |       Yes            |
 * |     ...    |        ...        |       ...       |     ...         |     ...    |       ...        |       ...           |      ...       |     ...       |       ...       |       ...      |      ...       |     ...       |    ...     |           ...           |      ...      |     ...      |             ...                 |       ...            |
 * ```
 */
@Suppress("ComponentNotRegistered")
class AutoHintValidationAction : ValidationAction<ValidationOfHintsDataframeRecord>() {

  override val outputFilePrefixName: String = "generatedValidationOfHints"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.auto.hint.validation.action.name")
  override val isNavigationRequired: Boolean = true
  override val pathToManualValidationDataset by lazy { Path(System.getProperty("manual.hint.validation.path")) }

  init {
    setUpSpinnerPanel(name)
  }

  override fun MutableList<ValidationOfHintsDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun CSVRecord.toDataframeRecord() = ValidationOfHintsDataframeRecord(get(0).toInt(), get(1), get(2).toInt(), get(3), get(4),
    get(5), get(6), get(7), get(8), get(9), get(10), get(11), get(12), get(13), get(14), get(15), get(16), get(17), get(18), get(19))

  private fun compareLengthCriterion(first: String, second: String): Boolean {
    val keywords = arrayOf(NEW_KEYWORD, CHANGED_KEYWORD, DELETED_KEYWORD)
    for (keyword in keywords) {
      val firstNumber = Regex("(?<=$keyword: )\\d+").find(first)?.value?.toInt()
      val secondNumber = Regex("(?<=$keyword: )\\d+").find(second)?.value?.toInt()
      if (firstNumber == null || secondNumber == null || firstNumber != secondNumber) return false
    }
    return true
  }

  private fun compareFeedbackTypeCriterion(first: String, second: String): Boolean {
    val firstList = first.split(",").map { it.trim() }
    val secondList = second.split(",").map { it.trim() }
    return firstList.size == secondList.size && firstList.containsAll(secondList)
  }

  override fun calculateAccuracy(
    manualRecords: List<ValidationOfHintsDataframeRecord>,
    autoRecords: List<ValidationOfHintsDataframeRecord>
  ) = ValidationOfHintsDataframeRecord(
    nextStepCodeHint = ACCURACY_KEYWORD,
    feedbackType = calculateCriterionAccuracy(
      { i -> manualRecords[i].feedbackType },
      { i -> autoRecords[i].feedbackType },
      manualRecords.size,
      { f, s -> compareFeedbackTypeCriterion(f, s) }
    ),
    information = calculateCriterionAccuracy(
      { i -> manualRecords[i].information },
      { i -> autoRecords[i].information },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    levelOfDetail = calculateCriterionAccuracy(
      { i -> manualRecords[i].levelOfDetail },
      { i -> autoRecords[i].levelOfDetail },
      manualRecords.size,
      { f, s -> compareCriterion(f, s, BOH_KEYWORD, HLD_KEYWORD) }
    ),
    personalized = calculateCriterionAccuracy(
      { i -> manualRecords[i].personalized },
      { i -> autoRecords[i].personalized },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    intersection = calculateCriterionAccuracy(
      { i -> manualRecords[i].intersection },
      { i -> autoRecords[i].intersection },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    appropriate = calculateCriterionAccuracy(
      { i -> manualRecords[i].appropriate },
      { i -> autoRecords[i].appropriate },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    specific = calculateCriterionAccuracy(
      { i -> manualRecords[i].specific },
      { i -> autoRecords[i].specific },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    misleadingInformation = calculateCriterionAccuracy(
      { i -> manualRecords[i].misleadingInformation },
      { i -> autoRecords[i].misleadingInformation },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    codeQuality = calculateCriterionAccuracy(
      { i -> manualRecords[i].codeQuality },
      { i -> autoRecords[i].codeQuality },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    kotlinStyle = calculateCriterionAccuracy(
      { i -> manualRecords[i].kotlinStyle },
      { i -> autoRecords[i].kotlinStyle },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    ),
    length = calculateCriterionAccuracy(
      { i -> manualRecords[i].correlationWithSteps },
      { i -> autoRecords[i].correlationWithSteps },
      manualRecords.size,
      { f, s -> compareLengthCriterion(f, s) }
    ),
    correlationWithSteps = calculateCriterionAccuracy(
      { i -> manualRecords[i].correlationWithSteps },
      { i -> autoRecords[i].correlationWithSteps },
      manualRecords.size,
      { f, s -> compareCriterion(f, s) }
    )
  )

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<ValidationOfHintsDataframeRecord> {
    val taskProcessor = TaskProcessor(task)
    val assistant = TaskBasedAssistant(taskProcessor)
    val project = task.project ?: error("Cannot get project")
    val eduState = project.eduState ?: error("Cannot get eduState for project ${project.name}")

    try {
      val response = assistant.getHint(task, eduState)
      val userCode = eduState.taskFile.getVirtualFile(project)?.getTextFromTaskTextFile() ?: error("Cannot get a user code")
      val solutionSteps = task.generatedSolutionSteps ?: error("Cannot get the solution steps")
      val taskDescription = taskProcessor.getTaskTextRepresentation()
      val textHint = response.textHint ?: error("Cannot get a text hint")
      val codeHint = response.codeHint ?: error("Cannot get a code hint")
      val hintType = processValidationHintForItsType(textHint, codeHint)
      val hintsValidation = processValidationHints(taskDescription, textHint, codeHint, userCode, solutionSteps)
      val dataframeRecord = Gson().fromJson(hintsValidation, ValidationOfHintsDataframeRecord::class.java)
      return listOf(dataframeRecord.apply {
        taskId = task.id
        taskName = task.name
        this.taskDescription = taskDescription
        this.solutionSteps = solutionSteps
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
        solutionSteps = "${EduAndroidAiAssistantValidationBundle.message("action.validation.error")} ${e.message}"
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
        manualValidationRecord.userCode,
        manualValidationRecord.solutionSteps
      )
      val dataframeRecord = Gson().fromJson(hintsValidation, ValidationOfHintsDataframeRecord::class.java)
      return dataframeRecord.apply {
        taskId = manualValidationRecord.taskId
        taskName = manualValidationRecord.taskName
        this.taskDescription = manualValidationRecord.taskDescription
        this.solutionSteps = manualValidationRecord.solutionSteps
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
        solutionSteps = "${EduAndroidAiAssistantValidationBundle.message("action.validation.error")} ${e.message}"
      )
    }
  }
}
