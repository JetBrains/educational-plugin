package com.jetbrains.edu.uiOnboarding

import java.awt.Image
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.ui.JBUI
import java.awt.Dimension
import java.io.InputStream
import javax.imageio.ImageIO
import kotlin.math.roundToInt

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

    val ZHABA_DIMENSION: Dimension get() = Dimension(zhabaScale(121), zhabaScale(107))
    val EYE_SHIFT: Int get() = zhabaScale(40)
    val SMALL_SHIFT: Int get() = zhabaScale(4)

    const val FRAME_DURATION: Long = 42 // is approximately 24 FPS
    const val JUMP_DURATION: Long = 300

    fun load(): EduUiOnboardingAnimationData? {
      fun loadImage(fileName: String): Image {
        val stream: InputStream? = this::class.java.getResourceAsStream("/images/$fileName.svg")
        val image = stream?.use { ImageIO.read(it) }
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