package com.jetbrains.edu.assistant.validation.util

data class StepsDataframeRecord(
  val taskId: Int,
  val taskName: String,
  val taskDescription: String,
  val prompt: String?,
  val steps: String?
) {
  constructor(
    taskId: Int,
    taskName: String,
    taskDescription: String,
    error: Throwable
  ): this(taskId, taskName, taskDescription, null, "Error while generating steps: ${error.message}")
}

data class CodeHintDataframeRecord(
  val taskId: Int,
  val taskName: String,
  val taskDescription: String,
  val taskAnalysisPrompt: String?,
  val steps: String?,
  val codeHintPrompt: String?,
  val userCode: String,
  val generatedCode: String?,
  val numberOfIssues: Int?,
  val issues: String?
) {
  constructor(
    taskId: Int,
    taskName: String,
    taskDescription: String,
    userCode: String,
    error: Throwable
  ) : this(
    taskId,
    taskName,
    taskDescription,
    null,
    null,
    null,
    userCode,
    "Error while generating hint: ${error.message}",
    null,
    null
  )
}

data class ValidationOfStepsDataframeRecord(
  var taskId: Int = 0,
  var taskName: String = "",
  var taskDescription: String = "",
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
)
