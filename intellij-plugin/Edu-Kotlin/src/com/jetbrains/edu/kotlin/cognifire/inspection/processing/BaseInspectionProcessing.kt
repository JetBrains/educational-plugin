package com.jetbrains.edu.kotlin.cognifire.inspection.processing

import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginModeProvider
import org.jetbrains.kotlin.psi.KtElement

abstract class BaseInspectionProcessing(private val element: KtElement) : InspectionProcessing {

  override fun isApplicable(): Boolean =
    if (KotlinPluginModeProvider.isK2Mode()) false
    else if (!element.isValid) false
    else if (!isApplicableLocal()) false
    else true

  abstract fun isApplicableLocal(): Boolean
}
