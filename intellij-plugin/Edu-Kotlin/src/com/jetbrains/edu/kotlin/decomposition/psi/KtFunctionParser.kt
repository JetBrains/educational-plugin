package com.jetbrains.edu.kotlin.decomposition.psi

import com.intellij.psi.PsiFile
import com.jetbrains.edu.decomposition.model.FunctionModel
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtFunctionParser : FunctionParser {
  override fun extractFunctionModels(files: List<PsiFile>): List<FunctionModel> =
    getFunctionsPsi(files).mapNotNull { it.asFunctionModel() }

  private fun KtNamedFunction.asFunctionModel() = FunctionModel(name.toString(), textOffset)
}