package com.jetbrains.edu.cognifire.codegeneration

import com.intellij.lang.Language
import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.edu.cognifire.utils.RedundantTodoCleaner
import com.intellij.openapi.project.Project
import com.jetbrains.edu.cognifire.inspection.InspectionProcessor
import com.jetbrains.edu.cognifire.models.CodeExpression
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.utils.toGeneratedCode
import com.jetbrains.edu.cognifire.utils.toPrompt
import com.jetbrains.educational.ml.cognifire.core.promptToCode.PromptToCodeAssistant
import com.jetbrains.educational.ml.cognifire.core.prodeSync.PromptSyncAssistant
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeContent
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

class CodeGenerator(
  private val promptExpression: PromptExpression,
  private val project: Project,
  private val language: Language,
  private val previousPromptToCode: PromptToCodeContent?,
  private val codeExpression: CodeExpression?
) {

  val finalPromptToCodeTranslation = updatePromptToCode()

  val promptToCodeLines = finalPromptToCodeTranslation
    .groupBy { it.promptLineNumber }
    .mapValues { promptGroup ->
      promptGroup.value.map { it.codeLineNumber }
    }
  val codeToPromptLines = finalPromptToCodeTranslation
    .groupBy { it.codeLineNumber }
    .mapValues { promptGroup ->
      promptGroup.value.map { it.promptLineNumber }
    }
  val generatedCode = finalPromptToCodeTranslation.toGeneratedCode()
  val generatedPrompt = finalPromptToCodeTranslation.toPrompt()

  private fun generatePromptToCode(): PromptToCodeResponse {
    val enumeratedPromptLines = getEnumeratedPromptLines(promptExpression)
    val signature = promptExpression.functionSignature.toString()
    return if (isCodeChanged()) {
      syncPrompt(
        previousPromptToCode ?: error("Invalid synchronization attempt"),
        codeExpression?.code?.lines()?.enumerate(0) ?: error("Invalid synchronization attempt"),
        getSetOfModifiedCodeLines(previousPromptToCode.toGeneratedCode(), codeExpression.code),
        signature
      )
    }
    else getCodeFromPrompt(signature, enumeratedPromptLines)
  }

  private fun updatePromptToCode(): PromptToCodeContent {
    val promptToCodeClearedFromWrongTodos =
      RedundantTodoCleaner.deleteWrongTodo(generatePromptToCode().content, promptExpression.functionSignature)
    return InspectionProcessor.applyInspections(
      promptToCodeClearedFromWrongTodos,
      promptExpression.functionSignature.toString(),
      project,
      language
    ) ?: promptToCodeClearedFromWrongTodos
  }

  private fun getCodeFromPrompt(functionSignature: String, enumeratedPromptLines: String) = runBlockingCancellable {
    PromptToCodeAssistant.translate(
      enumeratedPromptLines,
      functionSignature
    )
      .getOrThrow()
  }

  private fun syncPrompt(previousPromptToCode: PromptToCodeContent, modifiedCode: String, modifiedCodeLineNumbers: Set<Int>, functionSignature: String) =
    runBlockingCancellable {
      PromptSyncAssistant.syncPrompt(previousPromptToCode, modifiedCode, modifiedCodeLineNumbers, functionSignature).getOrThrow()
    }

  private fun getSetOfModifiedCodeLines(code: String, modifiedCode: String): Set<Int> {
    val oldLines = code.lines()
    val newLines = modifiedCode.lines()
    val maxLength = maxOf(oldLines.size, newLines.size)

    return (0 until maxLength)
      .filter { i -> oldLines.getOrNull(i) != newLines.getOrNull(i) }
      .toSet()
  }

  private fun getEnumeratedPromptLines(promptExpression: PromptExpression): String {
    val promptLines = promptExpression.prompt.lines().filter { it.isNotBlank() }
    val codeLines = promptExpression.code.lines().filter { it.isNotBlank() }

    return promptLines.enumerate(0) + "\n" +
           codeLines.enumerate(promptLines.size)
  }

  private fun List<String>.enumerate(startIndex: Int): String {
    return mapIndexed { index, line -> "${index + startIndex}: $line" }
      .joinToString("\n")
  }

  fun isCodeChanged() =
    previousPromptToCode != null && codeExpression?.code != null
    && previousPromptToCode.toGeneratedCode() != codeExpression.code.trimStartLines()
    && previousPromptToCode.toPrompt().trim() == promptExpression.prompt.trim()

  private fun String.trimStartLines() = this.lines().joinToString("\n") { it.trimStart() }
}