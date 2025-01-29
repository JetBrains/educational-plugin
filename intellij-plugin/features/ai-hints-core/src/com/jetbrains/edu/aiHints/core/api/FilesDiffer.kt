package com.jetbrains.edu.aiHints.core.api

import com.intellij.psi.PsiFile

interface FilesDiffer {
  /**
   * Returns names of functions that have different implementations between two [PsiFile]s.
   *
   * Example use case: identifying the list of function names that are changed in the [com.jetbrains.educational.ml.hints.hint.CodeHint].
   *
   * @param considerParameters Whether to consider changes in the parameter list.
   *
   * @see com.jetbrains.edu.aiHints.kotlin.impl.KtFilesDiffer.findChangedMethods
   */
  fun findChangedMethods(before: PsiFile, after: PsiFile, considerParameters: Boolean = false): List<String>
}