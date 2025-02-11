package com.jetbrains.edu.aiHints.kotlin.impl

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.asSafely
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.aiHints.core.api.StringExtractor
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings
import com.jetbrains.edu.aiHints.core.context.SignatureSource
import com.jetbrains.edu.aiHints.kotlin.impl.KtFunctionSignaturesManager.generateSignature
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

object KtStringExtractor : StringExtractor {
  @RequiresReadLock
  override fun getFunctionsToStringsMap(psiFile: PsiFile): FunctionsToStrings {
    val signatureToStrings = psiFile.findAllFunctions().mapNotNull { function ->
      val signature = function.generateSignature(SignatureSource.MODEL_SOLUTION) ?: return@mapNotNull null
      val stringTemplateExpressions = function.collectStringTemplateExpressions()
      val referredStringValues = function.collectReferredStringValues()
      val allStrings = (stringTemplateExpressions + referredStringValues).distinct()
      signature to allStrings
    }.toMap()
    return FunctionsToStrings(signatureToStrings)
  }

  private fun PsiFile.findAllFunctions(): Collection<KtNamedFunction> =
    PsiTreeUtil.findChildrenOfType(this, KtNamedFunction::class.java)

  private fun KtNamedFunction.collectStringTemplateExpressions(): List<String> =
    PsiTreeUtil.findChildrenOfType(this, KtStringTemplateExpression::class.java)
      .map { it.text }

  private fun KtNamedFunction.collectReferredStringValues(): List<String> =
    PsiTreeUtil.findChildrenOfType(this, KtReferenceExpression::class.java)
      .mapNotNull {
        analyze(it) {
          val ktProperty = it.mainReference.resolveToSymbol()?.psi.asSafely<KtProperty>() ?: return@analyze null
          val ktStringTemplateExpression = ktProperty.initializer.asSafely<KtStringTemplateExpression>() ?: return@analyze null
          ktStringTemplateExpression.text
        }
      }
}