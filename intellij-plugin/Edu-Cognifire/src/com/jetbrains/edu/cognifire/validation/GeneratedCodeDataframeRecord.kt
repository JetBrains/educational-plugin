package com.jetbrains.edu.cognifire.validation

/**
 * Data class representing a record in the generated code dataframe.
 *
 * @param taskId The ID of the task associated with this record.
 * @param function The function signature.
 * @param modelSolution The model solution code for the function.
 * @param prompt The prompt provided to generate the code, if any.
 * @param code The code prompt provided to generate the code, if any.
 * @param unparsableSentences Sentences from the prompt that could not be parsed, if any.
 * @param generatedCode The actual generated code, if any.
 * @param hasErrors Indicates whether TODOs were detected in the generated code.
 */
data class GeneratedCodeDataframeRecord(
  val taskId: Int,
  val function: String,
  val modelSolution: String,
  val prompt: String? = null,
  val code: String? = null,
  val unparsableSentences: String? = null,
  val generatedCode: String? = null,
  val hasErrors: Boolean = false,
  val exceptionMessage: String? = null,
) {
  constructor(
    taskId: Int,
    function: String,
    modelSolution: String,
    prompt: String,
    code: String,
    error: Throwable
  ) : this(
    taskId,
    function,
    modelSolution,
    prompt,
    code,
    exceptionMessage = "Error while generating code: ${error.message}",
  )

  constructor(
    taskId: Int,
    function: String,
    modelSolution: String
  ) : this(
    taskId,
    function,
    modelSolution,
    exceptionMessage = "Error while generating prompt",
  )
}
