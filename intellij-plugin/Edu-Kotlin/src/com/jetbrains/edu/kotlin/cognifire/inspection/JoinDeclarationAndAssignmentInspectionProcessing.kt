package com.jetbrains.edu.kotlin.cognifire.inspection

import org.jetbrains.kotlin.idea.intentions.JoinDeclarationAndAssignmentIntention
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.InspectionLikeProcessingForElement
import org.jetbrains.kotlin.psi.KtProperty

class JoinDeclarationAndAssignmentInspectionProcessing : InspectionLikeProcessingForElement<KtProperty>(KtProperty::class.java) {
  override fun isApplicableTo(element: KtProperty, settings: ConverterSettings): Boolean {
    return JoinDeclarationAndAssignmentIntention().applicabilityRange(element) != null
  }

  override fun apply(element: KtProperty) {
    JoinDeclarationAndAssignmentIntention().applyTo(element, null)
  }
}
