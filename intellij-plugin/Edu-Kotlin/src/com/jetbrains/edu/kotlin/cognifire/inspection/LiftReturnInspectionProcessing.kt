package com.jetbrains.edu.kotlin.cognifire.inspection

import org.jetbrains.kotlin.idea.inspections.LiftReturnOrAssignmentInspection
import org.jetbrains.kotlin.idea.k2.refactoring.util.BranchedFoldingUtils
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.InspectionLikeProcessingForElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.idea.inspections.LiftReturnOrAssignmentInspection.Util.LiftType.LIFT_RETURN_OUT

class LiftReturnInspectionProcessing : InspectionLikeProcessingForElement<KtExpression>(KtExpression::class.java) {

  override fun isApplicableTo(element: KtExpression, settings: ConverterSettings): Boolean {
    val state = LiftReturnOrAssignmentInspection.Util.getState(element, skipLongExpressions = false) ?: return false
    return state.any { it.liftType == LIFT_RETURN_OUT && it.isSerious }
  }

  override fun apply(element: KtExpression) {
    BranchedFoldingUtils.foldToReturn(element)
  }
}
