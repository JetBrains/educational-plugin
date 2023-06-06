package com.jetbrains.edu.learning.editor

import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.psi.PsiErrorElement
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.getTaskFile

class EduHighlightErrorFilter : HighlightErrorFilter() {
  override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
    val file = element.containingFile ?: return true
    val virtualFile = file.virtualFile ?: return true
    val taskFile = virtualFile.getTaskFile(element.project)
    return taskFile == null || taskFile.errorHighlightLevel !== EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION
  }
}
