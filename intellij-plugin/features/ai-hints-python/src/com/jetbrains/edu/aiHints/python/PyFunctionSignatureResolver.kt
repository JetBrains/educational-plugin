package com.jetbrains.edu.aiHints.python

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.edu.aiHints.core.FunctionSignatureResolver
import com.jetbrains.edu.aiHints.python.PyHintsUtils.functions

class PyFunctionSignatureResolver : FunctionSignatureResolver {
  override fun getFunctionBySignature(psiFile: PsiFile, functionName: String): PsiElement? {
    return psiFile.functions().firstOrNull { it.name == functionName }
  }
}