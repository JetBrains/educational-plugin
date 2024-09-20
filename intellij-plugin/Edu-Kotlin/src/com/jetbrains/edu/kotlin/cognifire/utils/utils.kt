package com.jetbrains.edu.kotlin.cognifire.utils

import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.models.FunctionArgument
import com.jetbrains.edu.cognifire.models.FunctionSignature
import org.jetbrains.kotlin.psi.KtNamedFunction

fun findBlock(
  element: PsiElement,
  step: (PsiElement) -> PsiElement?,
  condition: (PsiElement) -> Boolean
): PsiElement? {
  var possibleBlock: PsiElement? = element
  while(possibleBlock != null && !condition(possibleBlock)) {
    possibleBlock = step(possibleBlock)
  }
  return possibleBlock
}

fun getFunctionSignature(containingFunction: KtNamedFunction): FunctionSignature {
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
