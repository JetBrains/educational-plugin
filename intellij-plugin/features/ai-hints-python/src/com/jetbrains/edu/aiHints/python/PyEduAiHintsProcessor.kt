package com.jetbrains.edu.aiHints.python

import com.intellij.psi.PsiElement
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.aiHints.core.api.*
import com.jetbrains.edu.aiHints.python.impl.*

class PyEduAiHintsProcessor : EduAIHintsProcessor {
  override fun getFilesDiffer(): FilesDiffer = PyFilesDiffer

  @Suppress("UNCHECKED_CAST")
  override fun getFunctionDiffReducer(): FunctionDiffReducer<PsiElement> = PyFunctionDiffReducer as FunctionDiffReducer<PsiElement>

  override fun getInspectionsProvider(): InspectionsProvider = PyInspectionsProvider

  override fun getFunctionSignatureManager(): FunctionSignaturesManager = PyFunctionSignaturesManager

  override fun getStringsExtractor(): StringExtractor = PyStringExtractor
}