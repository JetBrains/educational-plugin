package com.jetbrains.edu.assistant.validation.actions.next.step.hint

import com.google.gson.Gson
import com.intellij.openapi.components.service
import com.jetbrains.edu.assistant.validation.accuracy.AccuracyCalculator
import com.jetbrains.edu.assistant.validation.actions.ValidationAction
import com.jetbrains.edu.assistant.validation.messages.EduAndroidAiAssistantValidationBundle
import com.jetbrains.edu.assistant.validation.processor.processValidationCompilationErrorHints
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
    val taskProcessor = TaskProcessor(task)
    val project = task.project ?: error("Cannot get project")
    val eduState = project.eduState ?: error("Cannot get eduState for project ${project.name}")
    runCheckAction(project)
    taskProcessor.getErrorDetails()?.let { errorDetails ->
      val response = project.service<TaskBasedAssistant>().getHint(taskProcessor, eduState)
      try {
        val userCode = response.taskFile?.getVirtualFile(project)?.getTextFromTaskTextFile() ?: error("Cannot get a user code")
        val codeHint = response.codeHint ?: error("Cannot get a code hint (${response.assistantError?.name ?: "no assistant error found"})")
        val textHint = response.textHint ?: error("Cannot get a text hint (${response.assistantError?.name ?: "no assistant error found"})")
        val hintsValidation = processValidationCompilationErrorHints(textHint, codeHint, userCode, errorDetails)
        val dataframeRecord = Gson().fromJson(hintsValidation, ValidationOfCompilationErrorHintsDataframeRecord::class.java)
        return listOf(dataframeRecord.apply {
          taskId = task.id
          taskName = task.name
          this.errorDetails = errorDetails
          this.userCode = userCode
          nextStepTextHint = textHint
          nextStepCodeHint = codeHint
        })
      }
      catch (e: Throwable) {
        return listOf(
          ValidationOfCompilationErrorHintsDataframeRecord(
            taskId = task.id,
            taskName = task.name,
            errorDetails = taskProcessor.getTaskTextRepresentation(),
            userCode = eduState.taskFile.getVirtualFile(project)?.getTextFromTaskTextFile() ?: "",
            nextStepCodeHint = response.codeHint ?: "",
            nextStepTextHint = response.textHint ?: "",
            errors = "${EduAndroidAiAssistantValidationBundle.message("action.validation.error")} ${e.message}"
          )
        )
      }
    } ?: run {
      return listOf(
        ValidationOfCompilationErrorHintsDataframeRecord(
          taskId = task.id,
          taskName = task.name,
          errorDetails = taskProcessor.getTaskTextRepresentation(),
          userCode = eduState.taskFile.getVirtualFile(project)?.getTextFromTaskTextFile() ?: "",
          errors = EduAndroidAiAssistantValidationBundle.message("action.validation.not.found.compilation.error")
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
