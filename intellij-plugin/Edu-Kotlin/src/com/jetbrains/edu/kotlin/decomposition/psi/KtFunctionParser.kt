package com.jetbrains.edu.kotlin.decomposition.psi

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.decomposition.model.FunctionModel
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtFunctionParser : FunctionParser {
  override fun extractFunctionModels(files: List<PsiFile>): List<FunctionModel> =
    getFunctionsPsi(files).mapNotNull { it.asFunctionModel() }

  override fun extractDependencies(files: List<PsiFile>): List<Pair<FunctionModel, List<FunctionModel>>>
  = getFunctionsPsi(files).map { caller -> Pair(caller.asFunctionModel(), PsiTreeUtil.findChildrenOfType(caller, KtNameReferenceExpression::class.java).map { it.asFunctionModelCall() }) }

  private fun getFunctionsPsi(files: List<PsiFile>)
  =  files.map { PsiTreeUtil.findChildrenOfType(it, KtNamedFunction::class.java) }.flatten()

  private fun KtNamedFunction.asFunctionModel() = FunctionModel(name.toString(), textOffset)

  private fun KtNameReferenceExpression.asFunctionModelCall() = FunctionModel(getReferencedName(), textOffset)


}