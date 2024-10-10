package com.jetbrains.edu.cognifire.codegeneration

import com.intellij.lang.Language
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.inspection.InspectionProcessor
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.educational.ml.cognifire.core.PromptToCodeAssistant
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

class CodeGenerator(promptExpression: PromptExpression, project: Project, language: Language) {
  private val enumeratedPromptLines = getEnumeratedPromptLines(promptExpression)

  private val promptToCodeTranslation: PromptToCodeResponse =
    getCodeFromPrompt(promptExpression.functionSignature.toString(), enumeratedPromptLines)

  private val promptToCodeAfterInspections: PromptToCodeResponse =
    InspectionProcessor.applyInspections(
      promptToCodeTranslation,
      promptExpression.functionSignature.toString(),
      project,
      language
    ) ?: promptToCodeTranslation

  var promptToCodeLines = promptToCodeAfterInspections
    .groupBy { it.promptLineNumber }
    .mapValues { promptGroup ->
      promptGroup.value.map { it.codeLineNumber }
    }
  var codeToPromptLines = promptToCodeAfterInspections
    .groupBy { it.codeLineNumber }
    .mapValues { promptGroup ->
      promptGroup.value.map { it.promptLineNumber }
    }
  val generatedCode =
    promptToCodeAfterInspections
      .distinctBy { it.codeLineNumber }
      .joinToString(System.lineSeparator()) {
        it.generatedCodeLine
      }

  private fun getCodeFromPrompt(functionSignature: String, enumeratedPromptLines: String) = runBlockingCancellable {
    PromptToCodeAssistant.generateCode(
      enumeratedPromptLines,
      functionSignature
    )
      .getOrThrow()
  }

  private fun getEnumeratedPromptLines(promptExpression: PromptExpression): String {
    val promptLines = promptExpression.prompt.lines()
    val codeLines = promptExpression.code.lines()

    return promptLines.enumerate(0) + System.lineSeparator() +
           codeLines.enumerate(promptLines.size + 1)
  }

  private fun List<String>.enumerate(startIndex: Int): String {
    return mapIndexed { index, line -> "${index + startIndex}: $line" }
      .joinToString(System.lineSeparator())
  }
}