package com.jetbrains.edu.kotlin.decomposition.psi

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.decomposition.parsers.FunctionParser
import org.jetbrains.kotlin.psi.KtNamedFunction

class KtFunctionParser : FunctionParser {
  override fun extractFunctionNames(files: List<PsiFile>): List<String> =
    files.map { PsiTreeUtil.findChildrenOfType(it, KtNamedFunction::class.java) }.flatten().mapNotNull { it.name }
}