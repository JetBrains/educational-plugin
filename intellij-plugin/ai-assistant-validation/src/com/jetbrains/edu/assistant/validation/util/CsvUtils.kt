package com.jetbrains.edu.assistant.validation.util

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
    prompt: String?,
    error: Throwable
  ): this(taskId, taskName, taskDescription, prompt, "Error while generating steps: ${error.message}", null)
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
  val generatedCode: String?,
  val numberOfIssues: Int?,
  val issues: String?
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
    null,
    null,
    null
  )
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
)

data class StudentSolutionRecord(
  val id: Int,
  val lessonName: String,
  val taskName: String,
  val code: String
)

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
  val information: String = "",
  val levelOfDetail: String = "",
  val personalized: String = "",
  val intersection: String = "",
  val appropriate: String = "",
  val specific: String = "",
  val misleadingInformation: String = "",
  val codeQuality: String = "",
  val kotlinStyle: String = "",
  val length: String = "",
  val correlationWithSteps: String = ""
)
