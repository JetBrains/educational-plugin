package com.jetbrains.edu.aiHints.python.impl

import com.intellij.psi.PsiElement
import com.jetbrains.edu.aiHints.core.api.FunctionDiffReducer

object PyFunctionDiffReducer : FunctionDiffReducer {
  override fun reduceDiffFunctions(function: PsiElement?, modifiedFunction: PsiElement): PsiElement = modifiedFunction
}