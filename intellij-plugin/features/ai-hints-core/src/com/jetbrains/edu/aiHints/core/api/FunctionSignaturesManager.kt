package com.jetbrains.edu.aiHints.core.api

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource

interface FunctionSignaturesManager {
  /**
   * A utility function that extracts function signatures from a [PsiFile] based on the specified [SignatureSource].
   *
   * Used for [com.jetbrains.educational.ml.hints.hint.CodeHint] modifications as well as building context information for LLM requests.
   */
  fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature>

  /**
   * Retrieves a first matched function's [PsiElement] by its name.
   *
   * Used for [com.jetbrains.educational.ml.hints.hint.CodeHint] modifications.
   */
  fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement?
}