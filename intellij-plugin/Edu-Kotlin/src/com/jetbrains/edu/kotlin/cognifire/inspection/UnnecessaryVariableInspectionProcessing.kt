package com.jetbrains.edu.kotlin.cognifire.inspection

import org.jetbrains.kotlin.idea.inspections.UnnecessaryVariableInspection
import org.jetbrains.kotlin.j2k.ConverterSettings
import org.jetbrains.kotlin.j2k.InspectionLikeProcessingForElement
import org.jetbrains.kotlin.psi.KtProperty

class UnnecessaryVariableInspectionProcessing : InspectionLikeProcessingForElement<KtProperty>(KtProperty::class.java) {
  override fun isApplicableTo(element: KtProperty, settings: ConverterSettings): Boolean {
    val inspection = UnnecessaryVariableInspection()
    inspection.reportImmediatelyReturnedVariables = true
    return inspection.isApplicable(element)
  }

  override fun apply(element: KtProperty) {
    return UnnecessaryVariableInspection().applyTo(element, element.project, null)
  }
}
