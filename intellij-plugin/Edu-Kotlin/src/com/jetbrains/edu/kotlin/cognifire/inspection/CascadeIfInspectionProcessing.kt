package com.jetbrains.edu.kotlin.cognifire.inspection

import org.jetbrains.kotlin.idea.base.psi.isOneLiner
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.intentions.IfToWhenIntention
import org.jetbrains.kotlin.idea.intentions.branches
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.InspectionLikeProcessingForElement
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.lastBlockStatementOrThis

class CascadeIfInspectionProcessing : InspectionLikeProcessingForElement<KtIfExpression>(KtIfExpression::class.java) {
  override fun isApplicableTo(element: KtIfExpression, settings: ConverterSettings): Boolean {
    val branches = element.branches
    if (branches.size <= 2) return false
    if (element.isOneLiner()) return false
    if (branches.any { it == null || it.lastBlockStatementOrThis() is KtIfExpression }) return false
    if (element.anyDescendantOfType<KtExpressionWithLabel> {
        it is KtBreakExpression || it is KtContinueExpression
      }) return false
    return true
  }

  override fun apply(element: KtIfExpression) {
    IfToWhenIntention().applyTo(element, null)
  }
}
