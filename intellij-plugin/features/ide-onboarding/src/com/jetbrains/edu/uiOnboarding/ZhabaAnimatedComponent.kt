package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.Disposable
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.uiOnboarding.ZhabaAnimationData.Companion.FRAMES_ON_GROUND_AFTER
import com.jetbrains.edu.uiOnboarding.ZhabaAnimationData.Companion.FRAMES_ON_GROUND_BEFORE
import com.jetbrains.edu.uiOnboarding.ZhabaAnimationData.Companion.ZHABA_X0
import com.jetbrains.edu.uiOnboarding.ZhabaAnimationData.Companion.ZHABA_X1
import com.jetbrains.edu.uiOnboarding.ZhabaAnimationData.Companion.ZHABA_Y0
import kotlinx.coroutines.delay
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Image
import java.awt.Point
import javax.swing.JComponent
import javax.swing.JFrame
import kotlin.math.floor


class ZhabaAnimatedComponent(
  private val frame: JFrame,
  private val zhabaAnimationData: ZhabaAnimationData,
  private val fromPoint: Point,
  private val toPoint: Point
) : JComponent(), Disposable {

  private var currentIndex: Int = 0
  private var currentDelta: Double = 0.0  // the number from 0 to 1 that means the progress of moving from fromPoint to toPoint

  private val framesCount = zhabaAnimationData.frames.size

  suspend fun animate() {
    if (framesCount <= 1) return

    val startTime = System.nanoTime() // System.currentTimeMillis() is affected when user changes the system time
    while (true) {
      val currentTime = System.nanoTime() - startTime
      val delta = currentTime.toDouble() / ANIMATION_TIME
      val frameIndex = floor(delta * (framesCount - 1)).toInt()

      this.currentIndex = frameIndex.coerceIn(0, framesCount - 1)
      this.currentDelta = delta.coerceIn(0.0, 1.0)

      repaint()
      if (frameIndex >= framesCount - 1) break


      delay(33) // 30 FPS
    }
  }

  override fun paintComponent(g: Graphics) {
    val image = zhabaAnimationData.frames.getOrNull(currentIndex) ?: return

    val p1 = fromPoint
    val p2 = toPoint

    val d = currentDelta

    val (shiftedX, shiftedY) = when {
      currentIndex < FRAMES_ON_GROUND_BEFORE -> p1.x - ZHABA_X0 to p1.y - ZHABA_Y0

      currentIndex >= framesCount - FRAMES_ON_GROUND_AFTER -> p2.x - ZHABA_X1 to p2.y - ZHABA_Y0

      else -> {
        val rescaledD = (d * framesCount - FRAMES_ON_GROUND_BEFORE) / (framesCount - FRAMES_ON_GROUND_BEFORE - FRAMES_ON_GROUND_AFTER)
        p1.x + rescaledD * (p2.x - ZHABA_X1 - p1.x + ZHABA_X0) to
          p1.y - ZHABA_Y0 + rescaledD * (p2.y - p1.y)
      }
    }

    val g2d = g as Graphics2D
    UIUtil.drawImage(g2d, image, shiftedX.toInt(), shiftedY.toInt(), null)

    super.paintComponent(g)
  }

  override fun dispose() {
    frame.layeredPane.remove(frame.layeredPane.components.find { it == this })
  }

  companion object {
    const val ANIMATION_TIME = 2_000_000_000// 3 seconds
  }
}

