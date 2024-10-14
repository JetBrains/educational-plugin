package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.edu.cognifire.GeneratedCodeParser
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.kotlin.cognifire.utils.UNIT_RETURN_VALUE
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtCallExpression

class KtGeneratedCodeParser : GeneratedCodeParser {
  override fun hasErrors(project: Project, generatedCode: String, functionSignature: FunctionSignature): Boolean {
    val psiFactory = PsiFileFactory.getInstance(project)
    val code = """
      $functionSignature {
            $generatedCode
        }
    """.trimIndent()
    val file = psiFactory.createFileFromText("fileName.kt", KotlinLanguage.INSTANCE, code)
    val todoStrings = mutableListOf<String>()
    file.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        if (element is KtCallExpression && element.calleeExpression?.text == TODO_MARKER) {
          val todoMessage = element.valueArguments.firstOrNull()?.getArgumentExpression()?.text ?: EMPTY_TODO
          if (todoMessage.contains(SPECIFY_RETURN_VALUE, ignoreCase = true) && functionSignature.returnType == UNIT_RETURN_VALUE) {
            deleteWrongTODO(file, project, element)
          } else {
            todoStrings.add(todoMessage)
          }
        }
        super.visitElement(element)
      }
    })
    return todoStrings.isNotEmpty()
  }

  private fun deleteWrongTODO(psiFile: PsiFile, project: Project, element: KtCallExpression) {
    WriteCommandAction.runWriteCommandAction(project, null, null, {
      element.delete()
    }, psiFile)
  }

  companion object {
    private const val TODO_MARKER = "TODO"
    private const val EMPTY_TODO = ""
    private const val SPECIFY_RETURN_VALUE = "specify the return value"
  }
}
