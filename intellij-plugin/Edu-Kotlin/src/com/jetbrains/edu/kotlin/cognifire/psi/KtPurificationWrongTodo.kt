package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.edu.cognifire.PurificationWrongTodo
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.cognifire.utils.toGeneratedCode
import com.jetbrains.edu.kotlin.cognifire.utils.UNIT_RETURN_VALUE
import com.jetbrains.edu.kotlin.cognifire.utils.createPsiFile
import com.jetbrains.edu.kotlin.cognifire.utils.getTODOMessage
import com.jetbrains.educational.ml.cognifire.responses.PromptToCodeResponse

class KtPurificationWrongTodo : PurificationWrongTodo {
  override fun deleteWrongTodo(project: Project, promptToCode: PromptToCodeResponse, functionSignature: FunctionSignature): PromptToCodeResponse {
    val lastCodeLine = promptToCode.maxOfOrNull { it.codeLineNumber } ?: return promptToCode
    val newPromptToCode = promptToCode.map { it.copy() }.toMutableList()
    val generatedCode = promptToCode.toGeneratedCode()
    val file = createPsiFile(project, functionSignature.toString(), generatedCode)
    file.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        element.getTODOMessage()?.let { todoMessage ->
          if (todoMessage.contains(SPECIFY_RETURN_VALUE, ignoreCase = true) && functionSignature.returnType == UNIT_RETURN_VALUE) {
            newPromptToCode.removeIf { it.generatedCodeLine == element.text && it.codeLineNumber == lastCodeLine }
          }
        }
        super.visitElement(element)
      }
    })
    return newPromptToCode
  }

  companion object {
    private const val SPECIFY_RETURN_VALUE = "specify the return value"
  }
}
