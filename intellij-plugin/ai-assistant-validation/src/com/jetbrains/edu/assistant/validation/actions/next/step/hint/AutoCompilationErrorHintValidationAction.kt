package com.jetbrains.edu.assistant.validation.actions.next.step.hint

import com.google.gson.Gson
import com.jetbrains.edu.assistant.validation.accuracy.AccuracyCalculator
import com.jetbrains.edu.assistant.validation.actions.ValidationAction
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.processor.processValidationCompilationErrorHints
import com.jetbrains.edu.assistant.validation.util.*
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.educational.ml.hints.core.TaskBasedAssistant
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.getTextFromTaskTextFile
import com.jetbrains.edu.learning.selectedTaskFile
import org.apache.commons.csv.CSVRecord
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import kotlin.io.path.Path

/**
 * The `Auto Validate Compilation Error Hints Generation` action runs an automatic validation of educational AI assistant generating text and code hints when a compilation error occurs.
 * The output data can be found in `educational-plugin/aiAssistantValidation/generatedValidationOfCompilationErrorHints.csv` by default, validation
 * output directory can be set up in `gradle.properties`.
 *
 * Example of the output data:
 * ```
 * |   taskId   |      taskName     |   errorDetails  |  userCode  |  nextStepTextHint |  nextStepCodeHint  | errors | comprehensible | unnecessaryContent | hasExplanation | explanationCorrect | hasFix | fixCorrect | correctImplementation | improvementOverTheOriginal |
 * |:----------:|:-----------------:|:---------------:|:----------:|:-----------------:|:------------------:|:------:|:--------------:|:------------------:|:--------------:|:------------------:|:------:|:----------:|:---------------------:|:--------------------------:|
 * | 1412191977 | ProgramEntryPoint |       ...       | package... | Replace the ...   | package ...        |   ...  |      Yes       |         No         |      BOH       |        Yes         |   No   |   Yes      |          Yes          |            No              |
 * |     ...    |        ...        |       ...       |     ...    |       ...         |       ...          |   ...  |      ...       |        ...         |       ...      |         ...        |   ...  |   ...      |          ...          |            ...             |
 * ```
 */
@Suppress("ComponentNotRegistered")
class AutoCompilationErrorHintValidationAction : ValidationAction<ValidationOfCompilationErrorHintsDataframeRecord>() {

  override val outputFilePrefixName: String = "generatedValidationOfCompilationErrorHints"
  override val name: String = EduAndroidAiAssistantValidationBundle.message("action.auto.compilation.error.hint.validation.action.name")
  override val isNavigationRequired: Boolean = true
  override val pathToLabelledDataset by lazy { Path(System.getProperty("manual.hint.validation.path")) }
  override val accuracyCalculator = AutoCompilationErrorHintValidationAccuracyCalculator()

  init {
    setUpSpinnerPanel(name)
  }

  override fun MutableList<ValidationOfCompilationErrorHintsDataframeRecord>.convertToDataFrame() = toDataFrame()

  override fun CSVRecord.toDataframeRecord() = ValidationOfCompilationErrorHintsDataframeRecord.buildFrom(this)

  override suspend fun buildRecords(task: EduTask, lesson: Lesson): List<ValidationOfCompilationErrorHintsDataframeRecord> {
    val taskProcessor = TaskProcessorImpl(task)
    val project = task.project ?: error("Cannot get project")
    runCheckAction(project)
    val errorDetails = taskProcessor.getErrorDetails()
    val response = TaskBasedAssistant(taskProcessor).getHint()
    return try {
      val userCode = taskProcessor.currentTaskFile?.getVirtualFile(project)?.getTextFromTaskTextFile() ?: error("Cannot get a user code")
      val codeHint =
        response.codeHint ?: error("Cannot get a code hint (${response.assistantException?.message ?: "no assistant error found"})")
      val textHint =
        response.textHint ?: error("Cannot get a text hint (${response.assistantException?.message ?: "no assistant error found"})")
      val hintsValidation = processValidationCompilationErrorHints(textHint, codeHint, userCode, errorDetails)
      val regex = "^(```\\w*)?(.*?)(```)?$".toRegex(RegexOption.DOT_MATCHES_ALL)
      val matchResult = regex.matchEntire(hintsValidation)?.groups?.get(2)?.value ?: hintsValidation
      val dataframeRecord = Gson().fromJson(matchResult, ValidationOfCompilationErrorHintsDataframeRecord::class.java)
      listOf(dataframeRecord.apply {
        taskId = task.id
        taskName = task.name
        this.errorDetails = errorDetails
        this.userCode = userCode
        nextStepTextHint = textHint
        nextStepCodeHint = codeHint
      })
    } catch (e: Throwable) {
      listOf(
        ValidationOfCompilationErrorHintsDataframeRecord(
          taskId = task.id,
          taskName = task.name,
          errorDetails = taskProcessor.getTaskTextRepresentation(),
          userCode = project.selectedTaskFile?.getVirtualFile(project)?.getTextFromTaskTextFile() ?: "",
          nextStepCodeHint = response.codeHint ?: "",
          nextStepTextHint = response.textHint ?: "",
          errors = "${EduAndroidAiAssistantValidationBundle.message("action.validation.error")} ${e.message}"
        )
      )
    }
  }

  override suspend fun buildRecords(manualValidationRecord: ValidationOfCompilationErrorHintsDataframeRecord): ValidationOfCompilationErrorHintsDataframeRecord {
    try {
      val hintsValidation = processValidationCompilationErrorHints(
        manualValidationRecord.nextStepTextHint,
        manualValidationRecord.nextStepCodeHint,
        manualValidationRecord.userCode,
        manualValidationRecord.errorDetails
      )
      val dataframeRecord = Gson().fromJson(hintsValidation, ValidationOfCompilationErrorHintsDataframeRecord::class.java)
      return dataframeRecord.apply {
        taskId = manualValidationRecord.taskId
        taskName = manualValidationRecord.taskName
        this.errorDetails = manualValidationRecord.errorDetails
        this.userCode = manualValidationRecord.userCode
        nextStepTextHint = manualValidationRecord.nextStepTextHint
        nextStepCodeHint = manualValidationRecord.nextStepCodeHint
      }
    } catch (e: Throwable) {
      return ValidationOfCompilationErrorHintsDataframeRecord(
        taskId = manualValidationRecord.taskId,
        taskName = manualValidationRecord.taskName,
        errorDetails = manualValidationRecord.errorDetails,
      )
    }
  }

  inner class AutoCompilationErrorHintValidationAccuracyCalculator : AccuracyCalculator<ValidationOfCompilationErrorHintsDataframeRecord> {

    override fun calculateValidationAccuracy(
      manualRecords: List<ValidationOfCompilationErrorHintsDataframeRecord>,
      autoRecords: List<ValidationOfCompilationErrorHintsDataframeRecord>
    ) = ValidationOfCompilationErrorHintsDataframeRecord(
      nextStepCodeHint = ACCURACY_KEYWORD,
      comprehensible = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::comprehensible) { f, s ->
        areSameCriteria(f, s!!)
      },
      unnecessaryContent = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::unnecessaryContent) { f, s ->
        areSameCriteria(f, s!!)
      },
      hasExplanation = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::hasExplanation) { f, s ->
        areSameCriteria(f, s!!)
      },
      explanationCorrect = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::explanationCorrect) { f, s ->
        areSameCriteria(f, s!!)
      },
      hasFix = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::hasFix) { f, s ->
        areSameCriteria(f, s!!)
      },
      fixCorrect = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::fixCorrect) { f, s ->
        areSameCriteria(f, s!!)
      },
      correctImplementation = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::correctImplementation) { f, s ->
        areSameCriteria(f, s!!)
      },
      improvementOverTheOriginal = calculateCriterionAccuracy(manualRecords, autoRecords, ValidationOfCompilationErrorHintsDataframeRecord::improvementOverTheOriginal) { f, s ->
        areSameCriteria(f, s!!)
      }
    )

    override fun calculateOverallAccuracy(records: List<ValidationOfCompilationErrorHintsDataframeRecord>) = ValidationOfCompilationErrorHintsDataframeRecord(
      nextStepCodeHint = ACCURACY_KEYWORD,
      comprehensible = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::comprehensible) { f -> isCorrectAnswer(f) },
      unnecessaryContent = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::unnecessaryContent) { f -> isCorrectAnswer(f, NO_KEYWORD) },
      hasExplanation = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::hasExplanation) { f -> isCorrectAnswer(f) },
      explanationCorrect = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::explanationCorrect) { f -> isCorrectAnswer(f) },
      hasFix = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::hasFix) { f -> isCorrectAnswer(f) },
      fixCorrect = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::fixCorrect) { f -> isCorrectAnswer(f) },
      correctImplementation = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::correctImplementation) { f -> isCorrectAnswer(f) },
      improvementOverTheOriginal = calculateCriterionResultAccuracy(records, ValidationOfCompilationErrorHintsDataframeRecord::improvementOverTheOriginal) { f -> isCorrectAnswer(f) },
    )
  }
}
