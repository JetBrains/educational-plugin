package com.jetbrains.edu.aiHints.python.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.api.FunctionSignaturesManager
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.edu.aiHints.python.impl.PyHintsUtils.functions
import com.jetbrains.edu.aiHints.python.impl.PyHintsUtils.generateSignature

object PyFunctionSignaturesManager : FunctionSignaturesManager {
  override fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature> {
    return psiFile.functions().mapNotNull { it.generateSignature(signatureSource) }
  }

  override fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement? {
    return psiFile.functions().firstOrNull { it.name == functionName }
  }
}