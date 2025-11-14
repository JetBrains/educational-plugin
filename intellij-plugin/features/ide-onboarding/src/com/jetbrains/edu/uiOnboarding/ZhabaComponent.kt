package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.FRAME_DURATION
import kotlinx.coroutines.delay
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

class ZhabaComponent(private val project: Project) : JComponent(), Disposable {

  var animation: EduUiOnboardingAnimation? = null

  private var stepIndex: Int = 0
  private var stepStartTime: Long = 0 // nano time of step start

  private var interrupted: Boolean = false

  override fun paintComponent(g: java.awt.Graphics) {
    val animation = this.animation ?: return

    val g2d = g as Graphics2D
    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)

    val step = animation.steps[stepIndex]

    val fromPointLocal = step.fromPoint.getPoint(this)
    val toPointLocal = step.toPoint.getPoint(this)
    
    val x1 = fromPointLocal.x
    val y1 = fromPointLocal.y
    
    val savedClip = g2d.clip
    if (step.visibleBounds != null) {
      g2d.clipRect(toPointLocal.x + step.visibleBounds.x, toPointLocal.y + step.visibleBounds.y, step.visibleBounds.width, step.visibleBounds.height)
    }

    if (step.notMoving) { // do less computation if there is no movement
      step.image.draw(g2d, x1, y1)
    }
    else {
      val x2 = toPointLocal.x
      val y2 = toPointLocal.y

      val timePassed = ((System.nanoTime() - stepStartTime) / 1_000_000.0 / step.duration).coerceIn(0.0, 1.0)
      val delta = step.transitionType.f(timePassed)

      val x = x1 + delta * (x2 - x1)
      val y = y1 + delta * (y2 - y1)
      step.image.draw(g2d, x.toInt(), y.toInt())
    }

    g2d.clip(savedClip)
    super.paintComponent(g)
  }

  /**
   * Starts the animation.
   * The animation continues until
   *  - the animation is over, and does not cycle (`animation.cycle == false`)
   *  - the component is disposed
   *  - the zhaba is interrupted by user (see [stop]), and the animation finished its another cycle.
   *
   * @return false if animation has stopped because of the interrupt.
   */
  suspend fun start(): Boolean {
    var index = 0
    while (true) {
      val animation = animation ?: return true

      runStep(index)

      index++
      if (index > animation.steps.lastIndex) {
        if (interrupted) {
          return false
        }

        if (animation.cycle) {
          index = 0
        }
        else {
          return true
        }
      }
    }
  }

  /**
   * Interrupt Zhaba after it finishes the animation cycle.
   * This makes method [start] return false.
   */
  fun stop() {
    interrupted = true
  }

  private suspend fun runStep(index: Int) {
    val animation = animation ?: return

    stepIndex = index
    stepStartTime = System.nanoTime()

    val step = animation.steps[index]
    if (step.notMoving) {
      repaint()
      delay(step.duration)
    }
    else {
      animateMotion(step)
    }
  }

  private suspend fun animateMotion(step: EduUiOnboardingAnimationStep) {
    while (true) {
      repaint()
      val timeLeft = step.duration - (System.nanoTime() - stepStartTime) / 1_000_000
      if (timeLeft <= 0) return
      val toWait = FRAME_DURATION.coerceAtMost(timeLeft)
      delay(toWait)
    }
  }

  override fun dispose() {
    animation = null
    val frame = WindowManager.getInstance().getFrame(project) ?: return
    frame.layeredPane.remove(this)
  }
}
