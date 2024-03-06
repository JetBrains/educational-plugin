package com.jetbrains.edu.java.learning.eduAssistant.psi.context

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiRecursiveElementVisitor
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionParameter
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignature
import com.jetbrains.edu.learning.courseFormat.eduAssistant.SignatureSource
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.FunctionSignaturesProvider

class JFunctionSignaturesProvider : FunctionSignaturesProvider {

  override fun getFunctionSignatures(psiFile: PsiFile, signatureSource: SignatureSource): List<FunctionSignature> {
    val functionSignatures = mutableListOf<FunctionSignature>()
    psiFile.accept(object : PsiRecursiveElementVisitor() {
      override fun visitElement(element: PsiElement) {
        when (element) {
          is PsiMethod -> {
            functionSignatures.add(
              FunctionSignature(
                element.name,
                element.parameterList.parameters.map { FunctionParameter(it.name, it.type.presentableText) },
                element.returnType?.presentableText ?: "void",
                signatureSource
              )
            )
          }
          else -> {
            super.visitElement(element)
          }
        }
      }
    })
    return functionSignatures
  }
}
