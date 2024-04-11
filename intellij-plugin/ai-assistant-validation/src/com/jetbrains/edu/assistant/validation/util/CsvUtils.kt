package com.jetbrains.edu.assistant.validation.util

import org.apache.commons.csv.CSVRecord

data class StepsDataframeRecord(
  val taskId: Int,
  val taskName: String,
  val taskDescription: String,
  val prompt: String?,
  val errors: String? = null,
  val steps: String?
) {
  constructor(
    taskId: Int,
    taskName: String,
    taskDescription: String,
    error: Throwable
  ): this(taskId, taskName, taskDescription, null, "Error while generating steps: ${error.message}", null)

  companion object {
    fun buildFrom(record: CSVRecord) = StepsDataframeRecord(
      record.get(0).toInt(),
      record.get(1),
      record.get(2),
      record.get(3),
      record.get(4),
      record.get(5)
    )
  }
}

data class CodeHintDataframeRecord(
  val taskId: Int,
  val taskName: String,
  val taskDescription: String,
  val taskAnalysisPrompt: String?,
  val steps: String?,
  val codeHintPrompt: String?,
  val userCode: String,
  val errors: String? = null,
  val generatedCode: String? = null,
  val numberOfIssues: Int? = null,
  val issues: String? = null
) {
  constructor(
    taskId: Int,
    taskName: String,
    taskDescription: String,
    taskAnalysisPrompt: String?,
    steps: String?,
    codeHintPrompt: String?,
    userCode: String,
    error: Throwable
  ) : this(
    taskId,
    taskName,
    taskDescription,
    taskAnalysisPrompt,
    steps,
    codeHintPrompt,
    userCode,
    "Error while generating hint: ${error.message}",
  )

  companion object {
    fun buildFrom(record: CSVRecord) = CodeHintDataframeRecord(
      record.get(0).toInt(), record.get(1), record.get(2), record.get(3),
      record.get(4), record.get(5), record.get(6),
      record.get(7), record.get(8), record.get(9).toInt(), record.get(10))
  }
}

data class ValidationOfStepsDataframeRecord(
  var taskId: Int = 0,
  var taskName: String = "",
  var taskDescription: String = "",
  var errors: String = "",
  var steps: String = "",
  var solution: String = "",
  val amount: Int = 0,
  val specifics: String = "",
  val independence: String = "",
  val codingSpecific: String = "",
  val direction: String = "",
  val misleadingInformation: String = "",
  val granularity: String = "",
  val kotlinStyle: String = ""
) {
  companion object {
    fun buildFrom(record: CSVRecord) = ValidationOfStepsDataframeRecord(
      record.get(0).toInt(), record.get(1), record.get(2), record.get(3), record.get(4),
      record.get(5), record.get(6).toInt(), record.get(7), record.get(8), record.get(9),
      record.get(10), record.get(11), record.get(12), record.get(13)
    )
  }
}

data class StudentSolutionRecord(
  val id: Int,
  val lessonName: String,
  val taskName: String,
  val code: String
) {
  companion object {
    fun buildFrom(record: CSVRecord) = StudentSolutionRecord(record.get(0).toInt(), record.get(1), record.get(2), record.get(3))
  }
}

data class ValidationOfHintsDataframeRecord(
  var taskId: Int = 0,
  var taskName: String = "",
  var taskDescription: String = "",
  var solutionSteps: String = "",
  var userCode: String = "",
  var nextStepTextHint: String = "",
  var nextStepCodeHint: String = "",
  var errors: String = "",
  var feedbackType : String = "",
  var information: String = "",
  var levelOfDetail: String = "",
  var personalized: String = "",
  var intersection: String = "",
  var appropriate: String = "",
  var specific: String = "",
  var misleadingInformation: String = "",
  var codeQuality: String = "",
  var kotlinStyle: String = "",
  var length: String = "",
  var correlationWithSteps: String = ""
) {
  companion object {
    fun buildFrom(record: CSVRecord) = ValidationOfHintsDataframeRecord(
      record.get(0).toInt(), record.get(1), record.get(2), record.get(3), record.get(4),
      record.get(5), record.get(6), record.get(7), record.get(8), record.get(9), record.get(10),
      record.get(11), record.get(12), record.get(13), record.get(14), record.get(15), record.get(16),
      record.get(17), record.get(18), record.get(19)
    )
  }
}


data class MultipleCodeHintDataframeRecord(
  val hintIndex: Int,
  val taskId: Int,
  val taskName: String,
  val taskDescription: String,
  val taskAnalysisPrompt: String? = null,
  val steps: String? = null,
  val codeHintPrompt: String? = null,
  val userCode: String?,
  val generatedCode: String?,
  val numberOfIssues: Int? = null,
  val issues: String? = null,
  val testStatus: String? = null,
  val errorMessage: String? = null
) {
  constructor(
    hintIndex: Int,
    taskId: Int,
    taskName: String,
    taskDescription: String,
    userCode: String,
    error: Throwable
  ) : this(
    hintIndex,
    taskId,
    taskName,
    taskDescription,
    userCode = userCode,
    generatedCode = "Error while generating hint: ${error.message}",
  )

  companion object {
    fun buildFrom(record: CSVRecord) = MultipleCodeHintDataframeRecord(
      record.get(0).toInt(), record.get(1).toInt(), record.get(2), record.get(3),
      record.get(4), record.get(5), record.get(6),
      record.get(7), record.get(8), record.get(9).toInt(), record.get(10), record.get(11)
    )
  }
}
