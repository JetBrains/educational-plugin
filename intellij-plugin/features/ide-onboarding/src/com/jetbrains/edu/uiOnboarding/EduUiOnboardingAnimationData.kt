package com.jetbrains.edu.uiOnboarding

import com.intellij.util.ImageLoader
import java.awt.Image
import com.intellij.openapi.diagnostic.logger
import java.awt.Dimension

class EduUiOnboardingAnimationData private constructor(
  val lookRight: Image,
  val lookLeft: Image,
  val lookDown: Image,
  val pointingLeft1: Image,
  val pointingLeft2: Image,
  val pointingRight1: Image,
  val pointingRight2: Image,
  val jumpRight1: Image,
  val jumpRight2: Image,
  val jumpLeft1: Image,
  val jumpLeft2: Image,
  val jumpDown: Image,
  val lookForward: Image,
  val lookUp: Image,
  val sad: Image,
  val winking: Image,
) {

  companion object {
    val LOG = logger<EduUiOnboardingAnimationData>()

    val ZHABA_DIMENSION: Dimension = Dimension(121, 107)
    const val EYE_SHIFT: Int = 40
    const val FRAME_DURATION: Long = 42 // is approximately 24 FPS
    const val JUMP_DURATION: Long = 300

    fun load(): EduUiOnboardingAnimationData? {
      fun loadImage(fileName: String): Image {
        val image = ImageLoader.loadFromResource("/images/$fileName.png", this::class.java)
        if (image == null) {
          LOG.error("Failed to load image '$fileName' for in ide onboarding")
          throw Exception("Failed to load image '$fileName' for in ide onboarding")
        }
        return image
      }

      return try {
        EduUiOnboardingAnimationData(
          loadImage("look-right"),
          loadImage("look-left"),
          loadImage("look-down"),
          loadImage("pointing-left-1"),
          loadImage("pointing-left-2"),
          loadImage("pointing-right-1"),
          loadImage("pointing-right-2"),
          loadImage("jump-right-1"),
          loadImage("jump-right-2"),
          loadImage("jump-left-1"),
          loadImage("jump-left-2"),
          loadImage("jump-down"),
          loadImage("look-forward"),
          loadImage("look-up"),
          loadImage("sad"),
          loadImage("winking"),
        )
      }
      catch (_: Exception) {
        null
      }
    }
  }
}