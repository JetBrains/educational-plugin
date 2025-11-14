package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.util.ImageLoader
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.zhabaScale
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.zhabaScaleWithoutIDEScale
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Rectangle

/**
 * Represents a Tode's image with a point at the top-left of its head.
 * If different images of the Toad are shown in such a way that their head-top-left point is placed in the same
 * screen point, then the Toad does not move visually.
 *
 */
class ZhabaImage(
  val image: Image,
  xShiftInImagePixels: Int,
  yShiftInImagePixels: Int
) {

  val originalSize = Dimension(image.getWidth(null), image.getHeight(null))

  val screenSize = Dimension(
    zhabaScaleWithoutIDEScale(originalSize.width),
    zhabaScaleWithoutIDEScale(originalSize.height),
  )

  val screenShiftX = zhabaScale(xShiftInImagePixels)
  val screenShiftY = zhabaScale(yShiftInImagePixels)

  fun draw(g2d: Graphics2D, x: Int, y: Int) {
    UIUtil.drawImage(
      g2d,
      image,
      Rectangle(x - screenShiftX, y - screenShiftY, screenSize.width, screenSize.height),
      Rectangle(0, 0, originalSize.width, originalSize.height),
      null
    )
  }

  companion object {

    fun load(fileName: String, shiftX: Int = 0, shiftY: Int = 0): ZhabaImage {
      val image = ImageLoader.loadFromResource("/images/$fileName.svg", this::class.java)
      if (image == null) {
        thisLogger().error("Failed to load image '$fileName' for Tode")
        throw Exception("Failed to load image '$fileName' for Tode")
      }
      return ZhabaImage(image, shiftX, shiftY)
    }

  }
}