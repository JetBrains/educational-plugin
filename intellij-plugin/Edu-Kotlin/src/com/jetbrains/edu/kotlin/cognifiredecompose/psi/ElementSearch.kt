package com.jetbrains.edu.kotlin.cognifiredecompose.psi

import com.intellij.psi.PsiElement
import com.jetbrains.edu.kotlin.cognifiredecompose.utils.findBlock
import org.jetbrains.kotlin.psi.KtNamedFunction

object ElementSearch {

  // Finds the block where all functions will be defined.
  fun findFunctionsBlock(element: PsiElement, step: (PsiElement) -> PsiElement?): KtNamedFunction? =
    findBlock(element, step) { it is KtNamedFunction && it.name == "functions"} as? KtNamedFunction
}
