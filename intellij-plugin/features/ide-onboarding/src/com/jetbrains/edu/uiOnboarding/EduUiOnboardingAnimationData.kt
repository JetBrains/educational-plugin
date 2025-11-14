package com.jetbrains.edu.uiOnboarding

import com.intellij.util.ImageLoader
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.ZHABA_SCALE
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.zhabaScale
import java.awt.Dimension
import java.awt.Image
import kotlin.math.roundToInt

class EduUiOnboardingAnimationData private constructor(
  val lookRight: ZhabaImage,
  val lookLeft: ZhabaImage,
  val lookDown: ZhabaImage,
  val pointingLeft1: ZhabaImage,
  val pointingLeft2: ZhabaImage,
  val pointingRight1: ZhabaImage,
  val pointingRight2: ZhabaImage,
  val jumpRight1: ZhabaImage,
  val jumpRight2: ZhabaImage,
  val jumpLeft1: ZhabaImage,
  val jumpLeft2: ZhabaImage,
  val jumpDown: ZhabaImage,
  val jumpUp: ZhabaImage,
  val lookForward: ZhabaImage,
  val lookUp: ZhabaImage,
  val sad: ZhabaImage,
  val winking: ZhabaImage,
) {

  companion object {
    private const val ZHABA_SCALE: Double = 0.80

    /**
     * The toad image is scaled to respect the IDE zoom, also it is scaled to [ZHABA_SCALE] for
     * design purposes to experiment with Tode size without modifying the original image.
     *
     * [zhabaScale] transforms lengths inside the original toad image to the on-screen pixel length.
     *
     * @param length size in pixels inside an original toad image.
     * @return size in pixels on the screen.
     */
    fun zhabaScale(length: Int): Int = (JBUI.scale(length) * ZHABA_SCALE).roundToInt()

    /**
     * See [zhabaScale], but IDE zoom is not taken into account.
     * The method is intended to be used with the width or height of an [Image], that has been loaded by [ImageLoader.loadFromResource].
     * Such loading already takes IDE zoom into account.
     */
    fun zhabaScaleWithoutIDEScale(length: Int): Int = (length * ZHABA_SCALE).roundToInt()

    val ZHABA_DIMENSION: Dimension get() = Dimension(zhabaScale(121), zhabaScale(107))
    val EYE_SHIFT: Int get() = zhabaScale(40)
    val SMALL_SHIFT: Int get() = zhabaScale(4)

    const val FRAME_DURATION: Long = 42 // is approximately 24 FPS
    const val JUMP_DURATION: Long = 300

    fun load(): EduUiOnboardingAnimationData? {
      return try {
        EduUiOnboardingAnimationData(
          ZhabaImage.load("look-right"),
          ZhabaImage.load("look-left"),
          ZhabaImage.load("look-down"),
          ZhabaImage.load("pointing-left-1", 110, 0),
          ZhabaImage.load("pointing-left-2", 110, 0),
          ZhabaImage.load("pointing-right-1"),
          ZhabaImage.load("pointing-right-2"),
          ZhabaImage.load("jump-right-1"),
          ZhabaImage.load("jump-right-2", 16, 80),
          ZhabaImage.load("jump-left-1"),
          ZhabaImage.load("jump-left-2", 0, 100),
          ZhabaImage.load("jump-down", 0, 100),
          ZhabaImage.load("jump-up", 0, 60),
          ZhabaImage.load("look-forward"),
          ZhabaImage.load("look-up"),
          ZhabaImage.load("sad"),
          ZhabaImage.load("winking", 0, 4),
        )
      }
      catch (_: Exception) {
        null
      }
    }
  }
}