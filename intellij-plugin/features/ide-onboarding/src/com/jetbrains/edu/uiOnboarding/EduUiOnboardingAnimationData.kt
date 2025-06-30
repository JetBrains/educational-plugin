package com.jetbrains.edu.uiOnboarding

import com.intellij.util.ImageLoader
import java.awt.Image
import com.intellij.openapi.diagnostic.logger
import java.awt.Dimension
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

    private const val BASE_WIDTH = 121
    private const val BASE_HEIGHT = 107
    private const val MIN_SCALE = 0.4
    private const val MAX_SCALE = 1.0
    private const val DEFAULT_SCALE = 0.80

    fun calculateScale(windowSize: Dimension): Double {
        val widthScale = windowSize.width / (BASE_WIDTH * 8.0)  // Toad should take ~1/8 of window width
        val heightScale = windowSize.height / (BASE_HEIGHT * 6.0)  // Toad should take ~1/6 of window height
        return (minOf(widthScale, heightScale) * DEFAULT_SCALE).coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun calculateDimension(windowSize: Dimension): Dimension {
        val scale = calculateScale(windowSize)
        return Dimension(
            (BASE_WIDTH * scale).roundToInt(),
            (BASE_HEIGHT * scale).roundToInt()
        )
    }

    fun zhabaScale(length: Int, windowSize: Dimension? = null): Int {
        val scale = windowSize?.let { calculateScale(it) } ?: DEFAULT_SCALE
        return (length * scale).roundToInt()
    }

    val ZHABA_DIMENSION: Dimension
        get() = calculateDimension(Dimension(1920, 1080))  // Default size for initial layout

    val EYE_SHIFT: Int = zhabaScale(40)

    const val FRAME_DURATION: Long = 42 // is approximately 24 FPS
    const val JUMP_DURATION: Long = 300

    fun load(): EduUiOnboardingAnimationData? {
      fun loadImage(fileName: String): Image {
        val image = ImageLoader.loadFromResource("/images/$fileName.svg", this::class.java)
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
