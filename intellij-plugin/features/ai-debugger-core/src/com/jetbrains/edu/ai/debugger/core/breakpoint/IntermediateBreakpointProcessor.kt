package com.jetbrains.edu.ai.debugger.core.breakpoint

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.ai.debugger.core.slicing.SliceManager

interface IntermediateBreakpointProcessor {

  fun findBreakpointLines(psiElement: PsiElement, document: Document, psiFile: PsiFile): List<Int>

  fun getCalleeExpressions(psiFile: PsiFile): List<PsiElement>

  fun getParentFunctionName(element: PsiElement): String?

  companion object {
    private val EP_NAME = LanguageExtension<IntermediateBreakpointProcessor>("Educational.intermediateBreakpointProcessor")

    @RequiresReadLock
    fun calculateIntermediateBreakpointPositions(
      virtualFile: VirtualFile,
      wrongCodeLineNumbers: List<Int>,
      project: Project,
      language: Language,
      withSlicing: Boolean = true
    ): List<Int> {
      val intermediateBreakpointProcessor = EP_NAME.forLanguage(language) ?: error("${EP_NAME.name} is not implemented for $language")
      val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
      val document = psiFile?.viewProvider?.document ?: return emptyList()
      return wrongCodeLineNumbers.map { wrongCodeLineNumber ->
        val psiElement = document.getPsiElementAtLine(psiFile, wrongCodeLineNumber) ?: return emptyList()
        val breakpointLines = intermediateBreakpointProcessor.findBreakpointLines(psiElement, document, psiFile)
        val functionCalls = getParentFunctionCallLines(psiElement, psiFile, document, intermediateBreakpointProcessor)
        if (withSlicing) {
          val slicingBp = SliceManager.getInstance(language).processSlice(psiElement, document, psiFile)
          // Combines slicing and heuristic intermediate breakpoints to select the most relevant ones, ensuring high precision.
          breakpointLines.intersect(slicingBp) + functionCalls
        } else {
          breakpointLines + functionCalls
        }
      }.flatten().distinct()
    }

    private fun Document.getPsiElementAtLine(psiFile: PsiFile, line: Int): PsiElement? {
      val lineStartOffset = getLineStartOffset(line)
      var result: PsiElement? = null
      psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
        override fun visitElement(element: PsiElement) {
          if (element.textRange.startOffset >= lineStartOffset && element !is PsiWhiteSpace && element !is LeafPsiElement) {
            result = element
            stopWalking()
          }
          super.visitElement(element)
        }
      })
      return result
    }

    private fun getParentFunctionCallLines(
      element: PsiElement,
      psiFile: PsiFile,
      document: Document,
      intermediateBreakpointProcessor: IntermediateBreakpointProcessor
    ): List<Int> {
      val parentFunctionName = intermediateBreakpointProcessor.getParentFunctionName(element) ?: return emptyList()
      return intermediateBreakpointProcessor.getCalleeExpressions(psiFile).filter { it.text == parentFunctionName }
        .map { document.getLineNumber(it.textRange.startOffset) }
    }

    fun Document.getAllReferencesLines(element: PsiElement): List<Int> =
      ReferencesSearch.search(element).findAll().mapNotNull {
        getStartLineNumber(it.element)
      }.distinct()

    fun Document.getStartLineNumber(element: PsiElement): Int = getLineNumber(element.textRange.startOffset)
  }
}
