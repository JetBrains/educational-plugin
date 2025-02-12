package com.jetbrains.edu.kotlin.cognifiredecompose.psi

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifiredecompose.models.FunctionExpression
import com.jetbrains.edu.cognifiredecompose.writers.FunctionWriter
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtPsiFactory

class KtFunctionWriter : FunctionWriter<FunctionExpression> {

  override fun addExpression(project: Project, element: PsiElement): FunctionExpression {
    val psiFactory = KtPsiFactory(project)

    val functionTemplate = getCodeTemplate()

    val newFunctionBlock = psiFactory.createExpression(functionTemplate) as? KtCallExpression ?: error("Failed to create new function block")
    val documentManager = PsiDocumentManager.getInstance(project)

    WriteCommandAction.runWriteCommandAction(project, null, null, {
      documentManager.commitAllDocuments()
      createElementParent(newFunctionBlock, element)
    })
    return FunctionExpression(
      ""
    )
  }

  private fun getCodeTemplate(): String {
    return GeneratorUtils.getInternalTemplateText(
      FUNCTION_BLOCK
    )
  }

  private fun createElementParent(newFunctionBlock: KtExpression, element: PsiElement): PsiElement {
    val functions = ElementSearch.findFunctionsBlock(element) { it.nextSibling }
    functions ?: error("Could not find functions block")
    val createdElement = functions.lastChild.addBefore(newFunctionBlock, functions.lastChild.lastChild)

    return createdElement
  }

  companion object {
    const val FUNCTION_BLOCK = "FunctionBlock.kt"
  }
}
