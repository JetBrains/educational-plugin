package com.jetbrains.edu.uiOnboarding

import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.zhabaScale
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.zhabaScaleWithoutIDEScale
import java.awt.Dimension
import java.awt.Image

class ZhabaImage(
  val image: Image,
  xShiftInPixels: Int = 0,
  yShiftInPixels: Int = 0
) {

  val size = Dimension(
    zhabaScaleWithoutIDEScale(image.getWidth(null)),
    zhabaScaleWithoutIDEScale(image.getHeight(null)),
  )

  val xShift = zhabaScale(xShiftInPixels)
  val yShift = zhabaScale(yShiftInPixels)
}