package com.jetbrains.edu.kotlin.cognifiredecompose.psi

import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifiredecompose.CognifireDecomposeDslPackageCallChecker
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KtCognifireDecomposeDslPackageCallChecker: CognifireDecomposeDslPackageCallChecker {
  override fun isCallFromCognifireDecomposeDslPackage(element: PsiElement): Boolean {
    val project = element.project
    val ktElement = element as? KtCallExpression ?: return false
    val service = project.serviceOrNull<KotlinCacheService>()
    if (service == null) {
      if (KotlinPluginModeProvider.isK2Mode()) {
        // TODO workaround to support K2, remove after EDU-7553 is fixed
        return true
      }
      error("KotlinCacheService is not found")
    }
    val bindingContext = service.getResolutionFacade(ktElement).analyze(ktElement)
    val resolvedCall = ktElement.calleeExpression.getResolvedCall(bindingContext) ?: return false
    return resolvedCall.resultingDescriptor.containingDeclaration.fqNameSafe == FqName(COGNIFIRE_DECOMPOSE_DSL_PACKAGE)
  }

  companion object {
    private const val COGNIFIRE_DECOMPOSE_DSL_PACKAGE = "org.jetbrains.academy.cognifiredecompose.dsl"
  }
}