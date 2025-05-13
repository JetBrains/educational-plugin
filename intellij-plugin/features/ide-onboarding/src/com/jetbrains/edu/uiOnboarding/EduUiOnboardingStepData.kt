package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import java.awt.Point

// copy-pasted from mono-repo
class EduUiOnboardingStepData(
  val builder: GotItComponentBuilder,
  val relativePoint: RelativePoint,
  // null means that the balloon should be shown in the center of the component referenced in relativePoint
  val position: Balloon.Position?,
  val zhaba: ZhabaComponent,
  val characterShift: Point // the position of the Zhaba itself inside the image
)