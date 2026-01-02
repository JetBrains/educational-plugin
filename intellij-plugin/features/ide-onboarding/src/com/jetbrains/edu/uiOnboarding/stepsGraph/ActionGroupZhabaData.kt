package com.jetbrains.edu.uiOnboarding.stepsGraph

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.BalloonBuilder
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.uiOnboarding.ZhabaComponent

class ActionGroupZhabaData(
  val builder: BalloonBuilder,
  val tooltipPoint: RelativePoint,
  // null means that the balloon should be shown in the center of the component referenced in tooltipPoint
  val position: Balloon.Position?,

  override val zhabaPoint: RelativePoint,
  override val zhaba: ZhabaComponent,
) : ZhabaDataWithComponent
