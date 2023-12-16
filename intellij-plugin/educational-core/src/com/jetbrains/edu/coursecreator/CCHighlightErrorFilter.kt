package com.jetbrains.edu.coursecreator

import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.psi.PsiErrorElement

class CCHighlightErrorFilter : HighlightErrorFilter() {
  override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
    val file = element.containingFile ?: return true
    val virtualFile = file.virtualFile ?: return true
    return !virtualFile.path.contains(CCUtils.GENERATED_FILES_FOLDER)
  }
}
