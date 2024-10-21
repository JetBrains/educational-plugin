package com.jetbrains.edu.cognifire.codegeneration

import com.intellij.openapi.progress.runBlockingCancellable
import com.jetbrains.edu.cognifire.utils.RedundantTodoCleaner
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.utils.toGeneratedCode
import com.jetbrains.educational.ml.cognifire.core.PromptToCodeAssistant
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

class CodeGenerator(promptExpression: PromptExpression) {
  private val enumeratedPromptLines = getEnumeratedPromptLines(promptExpression)

  private val promptToCodeTranslation: PromptToCodeResponse =
    getCodeFromPrompt(promptExpression.functionSignature.toString(), enumeratedPromptLines)

  private val promptToCodeClearedFromWrongTodos =
    RedundantTodoCleaner.deleteWrongTodo(promptToCodeTranslation, promptExpression.functionSignature)

  val promptToCodeLines = promptToCodeClearedFromWrongTodos
    .groupBy { it.promptLineNumber }
    .mapValues { promptGroup ->
      promptGroup.value.map { it.codeLineNumber }
    }
  val codeToPromptLines = promptToCodeClearedFromWrongTodos
    .groupBy { it.codeLineNumber }
    .mapValues { promptGroup ->
      promptGroup.value.map { it.promptLineNumber }
    }
  val generatedCode = promptToCodeClearedFromWrongTodos.toGeneratedCode()

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