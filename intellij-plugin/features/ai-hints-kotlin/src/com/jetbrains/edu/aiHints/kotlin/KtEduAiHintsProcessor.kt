package com.jetbrains.edu.aiHints.kotlin

import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.aiHints.core.api.*
import com.jetbrains.edu.aiHints.kotlin.impl.*

class KtEduAiHintsProcessor : EduAIHintsProcessor {
  override fun getFilesDiffer(): FilesDiffer = KtFilesDiffer

  override fun getFunctionDiffReducer(): FunctionDiffReducer = KtFunctionDiffReducer

  override fun getInspectionsProvider(): InspectionsProvider = KtInspectionsProvider

  override fun getFunctionSignatureManager(): FunctionSignaturesManager = KtFunctionSignaturesManager

  override fun getStringsExtractor(): StringExtractor = KtStringExtractor
}