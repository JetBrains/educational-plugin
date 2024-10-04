package com.jetbrains.edu.kotlin.cognifire.psi

import ai.grazie.utils.dropPostfix
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentOfType
import com.jetbrains.edu.cognifire.PromptExpressionParser
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.cognifire.models.PromptExpression
import com.jetbrains.edu.cognifire.models.FunctionArgument
import com.jetbrains.edu.cognifire.utils.isPromptBlock
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.psi.psiUtil.endOffset

class KtPromptExpressionParser : PromptExpressionParser {
  override fun parsePromptExpression(promptExpression: PsiElement): PromptExpression? {
    if (!promptExpression.isPromptBlock() ||
        promptExpression !is KtCallExpression ||
        existsNestedPromptExpressions(promptExpression)
    ) {
      return null
    }
    val containingFunction = promptExpression.findParentOfType<KtNamedFunction>() ?: return null

    val promptPromptPsi = promptExpression.valueArguments.firstOrNull()
    val promptCodeBlockPsi = promptExpression.lambdaArguments.firstOrNull()?.getLambdaExpression()

    val promptPromptText = promptPromptPsi?.text ?: ""
    val trimmedPromptPromptText = promptPromptText.trimStart(QUOTE_CHAR).trimStart()

    val trimmedOffset = promptPromptText.length - trimmedPromptPromptText.length

    return PromptExpression(
      getFunctionSignature(containingFunction),
      (promptPromptPsi?.textOffset ?: 0) + trimmedOffset,
      promptExpression.startOffset,
      promptExpression.endOffset,
      trimmedPromptPromptText.dropPostfix(TRIM_INDENT_POSTFIX).dropPostfix(QUOTE_POSTFIX),
      promptCodeBlockPsi?.bodyExpression?.text ?: ""
    )
  }

  private fun getFunctionSignature(containingFunction: KtNamedFunction): FunctionSignature {
    val containingFunctionParameters =
      containingFunction.valueParameterList?.parameters
        ?.map {
          FunctionArgument(
            it?.name ?: "",
            it?.typeReference?.text ?: ""
          )
        } ?: emptyList()

    return FunctionSignature(
      containingFunction.name ?: "",
      containingFunctionParameters,
      containingFunction.typeReference?.text ?: "Unit"
    )
  }

  private fun existsNestedPromptExpressions(promptExpression: KtCallExpression) =
    PsiTreeUtil.findChildrenOfType(promptExpression, KtCallExpression::class.java).any { it.isPromptBlock() }

  companion object {
    private const val QUOTE_CHAR = '"'
    private const val TRIM_INDENT_POSTFIX = ".trimIndent()"
    private const val QUOTE_POSTFIX = "\"\"\""
  }
}