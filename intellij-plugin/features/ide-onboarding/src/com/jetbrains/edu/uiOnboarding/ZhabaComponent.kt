package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.WindowManager
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.FRAME_DURATION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.RERUN_TRANSITION
import kotlinx.coroutines.*
import java.awt.Component
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.JComponent
import javax.swing.SwingUtilities

class ZhabaComponent(private val project: Project) : JComponent(), Disposable {

  var animation: EduUiOnboardingAnimation? = null

  private var stepIndex: Int = 0
  private var stepStartTime: Long = 0 // nano time of step start

  private var interruptionReason: String? = null
  private var animationJob: Job? = null

  override fun contains(x: Int, y: Int): Boolean = false

  init {
    isFocusable = false
    isOpaque = false
    actionMap.clear()
    inputMap.clear()
  }

  override fun processMouseEvent(e: MouseEvent?) { }
  override fun processMouseMotionEvent(e: MouseEvent?) { }
  override fun processMouseWheelEvent(e: MouseWheelEvent?) { }

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
   *  - the zhaba is interrupted by user (see [stop]).
   *    If [animation]'s `mayBeInterruptedInsideCycle == true`, the interruption is immediate.
   *    Otherwise, the animation continues until the end of the current cycle.
   *
   * @return null if animation was not interrupted or the interruption reason, if the animation was interrupted.
   */
  suspend fun start(cs: CoroutineScope): String? {
    val animationJob = cs.launch(Dispatchers.EDT) {
      runAnimation()
    }

    this.animationJob = animationJob

    animationJob.join()

    return interruptionReason
  }

  private suspend fun runAnimation() {
    var index = 0
    while (true) {
      val animation = animation ?: return

      runStep(index)

      index++
      if (index > animation.steps.lastIndex) {
        if (interruptionReason != null) {
          thisLogger().info("Toad interrupted at the end of cycle: $interruptionReason")
          throw CancellationException()
        }

        if (animation.cycle) {
          index = 0
        }
        else {
          return
        }
      }
    }
  }

  /**
   * Interrupt Zhaba. If [animation]'s `mayBeInterruptedInsideCycle == true`, the interruption is immediate.
   * Otherwise, the interruption is after the animation finishes the animation cycle.
   *
   * This call makes method [start] return false.
   * * [reason] contains some arbitrary string, it will be available to the caller of the [start] method after it returns.
   */
  fun stop(reason: String) {
    interruptionReason = reason

    if (animation?.mayBeInterruptedInsideCycle == true) {
      animationJob?.cancel()
      thisLogger().info("Toad will be interrupted immediately: $reason animation=$animation component=${this.hashCode()}")
    }
    else {
      thisLogger().info("Toad will be interrupted at the end of cycle: $reason")
    }
  }

  /**
   * The Toad is interrupted if the tracked component is moved or resized or its visibility changed.
   */
  fun trackComponent(component: Component) {
    val layeredPane = WindowManager.getInstance().getFrame(project)?.layeredPane ?: return

    fun position(): Point = SwingUtilities.convertPoint(component, 0, 0, layeredPane)
    var currentPosition = position()
    var currentSize = component.size

    fun stop() {
      stop(RERUN_TRANSITION)
    }

    fun stopIfNeeded() {
      val newPosition = position()
      val newSize = component.size
      if (currentPosition != newPosition || currentSize != newSize) {
        stop()

        currentPosition = newPosition
        currentSize = newSize
      }
    }

    val componentListener = object : ComponentAdapter() {
      override fun componentHidden(e: ComponentEvent?) { stop() }
      override fun componentShown(e: ComponentEvent?) { stop() }
      override fun componentMoved(e: ComponentEvent?) { stop() }
      override fun componentResized(e: ComponentEvent?) { stop() }
    }
    val hierarchyListener = HierarchyListener { stop() }
    val hierarchyBoundsListener = object : HierarchyBoundsAdapter() {
      override fun ancestorMoved(e: HierarchyEvent?) { stopIfNeeded() }
      override fun ancestorResized(e: HierarchyEvent?) { stopIfNeeded() }
    }

    Disposer.register(this) {
      thisLogger().info("Disposing ZhabaComponent for component=${this.hashCode()}")
      component.removeComponentListener(componentListener)
      component.removeHierarchyListener(hierarchyListener)
      component.removeHierarchyBoundsListener(hierarchyBoundsListener)
    }
    component.addComponentListener(componentListener)
    component.addHierarchyListener(hierarchyListener)
    component.addHierarchyBoundsListener(hierarchyBoundsListener)
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
