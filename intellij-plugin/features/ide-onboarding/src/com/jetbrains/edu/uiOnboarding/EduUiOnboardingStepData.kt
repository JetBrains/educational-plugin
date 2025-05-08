package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint

// copy-pasted from mono-repo
class EduUiOnboardingStepData(
  val builder: GotItComponentBuilder,
  val relativePoint: RelativePoint,
  // null means that the balloon should be shown in the center of the component referenced in relativePoint
  val position: Balloon.Position?,
  val zhaba: ZhabaComponent,
)