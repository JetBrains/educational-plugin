package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.GotItComponentBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaDataWithComponent

class EduUiOnboardingStepData(
  val builder: GotItComponentBuilder,
  val tooltipPoint: RelativePoint,
  override val zhabaPoint: RelativePoint,
  // null means that the balloon should be shown in the center of the component referenced in relativePoint
  val position: Balloon.Position?,
  override val zhaba: ZhabaComponent,
) : ZhabaDataWithComponent