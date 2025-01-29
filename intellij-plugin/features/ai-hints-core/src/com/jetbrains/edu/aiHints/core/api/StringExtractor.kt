package com.jetbrains.edu.aiHints.core.api

import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings

interface StringExtractor {
  /**
   * A utility function that creates a mapping of functions to their string representations for the given [PsiFile].
   * Used to build context information for LLM requests.
   *
   * @see [com.jetbrains.educational.ml.hints.context.StepByStepNextCodeStepContext]
   * @see [com.jetbrains.educational.ml.hints.context.TaskCompletionGuidanceContext]
   * @see [com.jetbrains.edu.aiHints.core.context.AuthorSolutionContext]
   */
  fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings
}