package com.jetbrains.edu.aiDebugging.kotlin.slice

import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.edu.aiDebugging.core.slicing.SliceManager
import org.jetbrains.kotlin.psi.KtFunction
import com.jetbrains.edu.aiDebugging.kotlin.slice.DependencyDirection.FORWARD
import com.jetbrains.edu.aiDebugging.kotlin.slice.DependencyDirection.BACKWARD

class KSliceManager : SliceManager {

  override fun processSlice(element: PsiElement, document: Document, psiFile: PsiFile): Set<Int> {
    val function = PsiTreeUtil.getParentOfType(element, KtFunction::class.java) ?: return emptySet()
    val analyzer = CodeDependencyAnalyzer()
    val forwardDependency = analyzer.processDependency(function, FORWARD).filter { it.key == element }.values.flatten()
    val backwardDependency = analyzer.processDependency(function, BACKWARD).filter { it.key == element }.values.flatten()
    return (forwardDependency + backwardDependency).map { document.getLineNumber(it.textRange.startOffset) }.toSet()
  }

}
