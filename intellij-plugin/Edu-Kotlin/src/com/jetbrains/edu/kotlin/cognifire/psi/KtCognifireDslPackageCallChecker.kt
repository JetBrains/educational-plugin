package com.jetbrains.edu.kotlin.cognifire.psi

import com.intellij.openapi.components.service
import com.intellij.psi.PsiElement
import com.jetbrains.edu.cognifire.CognifireDslPackageCallChecker
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class KtCognifireDslPackageCallChecker : CognifireDslPackageCallChecker {
  override fun isCallFromCognifireDslPackage(element: PsiElement): Boolean {
    val project = element.project
    val ktElement = element as? KtCallExpression ?: return false
    val bindingContext = try {
      project.service<KotlinCacheService>().getResolutionFacade(listOf(ktElement)).analyze(ktElement)
    } catch (_: Exception) {
      return true
    }
    return element.calleeExpression.getResolvedCall(bindingContext)?.resultingDescriptor?.containingDeclaration?.fqNameSafe ==
      FqName(COGNIFIRE_DSL_PACKAGE)
  }

  companion object {
    private const val COGNIFIRE_DSL_PACKAGE = "org.jetbrains.academy.cognifire.dsl"
  }
}
