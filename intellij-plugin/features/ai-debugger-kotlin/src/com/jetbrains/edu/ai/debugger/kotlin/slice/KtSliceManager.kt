package com.jetbrains.edu.ai.debugger.kotlin.slice

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.ai.debugger.core.slicing.SliceManager
import com.jetbrains.edu.ai.debugger.kotlin.slice.DependencyDirection.BACKWARD
import com.jetbrains.edu.ai.debugger.kotlin.slice.DependencyDirection.FORWARD
import org.jetbrains.kotlin.psi.KtFunction

class KtSliceManager : SliceManager {
  override fun processSlice(element: PsiElement, document: Document, psiFile: PsiFile): Set<Int> {
    val function = PsiTreeUtil.getParentOfType(element, KtFunction::class.java) ?: return emptySet()
    val analyzer = CodeDependencyAnalyzer()
    val forwardDependency = analyzer.processDependency(function, FORWARD).filter { it.key == element }.values.flatten()
    val backwardDependency = analyzer.processDependency(function, BACKWARD).filter { it.key == element }.values.flatten()
    return (forwardDependency + backwardDependency).mapNotNull { document.getLineNumberSafe(it.textRange.startOffset) }.toSet()
  }
}
