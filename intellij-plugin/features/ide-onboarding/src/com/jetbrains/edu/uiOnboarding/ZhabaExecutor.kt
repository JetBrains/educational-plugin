// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.ide.ui.UISettingsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.use
import com.intellij.openapi.wm.WindowManager
import com.jetbrains.edu.uiOnboarding.stepsGraph.*
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.RERUN_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.STEP_UNAVAILABLE_TRANSITION
import kotlinx.coroutines.CoroutineScope
import java.awt.Component
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.HierarchyBoundsAdapter
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import javax.swing.JLayeredPane

class ZhabaExecutor(
  private val project: Project,
  private val graph: ZhabaGraph,
  private val cs: CoroutineScope,
  parentDisposable: Disposable
) : Disposable.Default {

  private var animationData: EduUiOnboardingAnimationData? = updateAnimationData()
  private var currentZhabaComponent: ZhabaComponent? = null

  init {
    Disposer.register(parentDisposable, this)

    // Listen to IDE Zoom changes and other changes that might affect UI components positions and sizes
    project.messageBus.connect(this).subscribe(UISettingsListener.TOPIC, UISettingsListener {
      updateAnimationData()
      changeZhabaLocation()
    })
  }

  private fun updateAnimationData(): EduUiOnboardingAnimationData? {
    val loadedData = EduUiOnboardingAnimationData.load()
    if (loadedData != null) {
      animationData = loadedData
    }
    else {
      thisLogger().error("Failed to reload EduUiOnboardingAnimationData after IDE UI settings changed, using the old data")
    }
    return animationData
  }

  /**
   * Notify zhaba that it has to change its location because of movements of UI components.
   */
  private fun changeZhabaLocation() {
    currentZhabaComponent?.stop()
  }

  suspend fun start(step: ZhabaStepBase) {
    var currentStep = step
    var currentData = step.typed().performStep(project, (animationData ?: return)) ?: return
    var currentTransition: String? = null

    while (true) {
      if (currentTransition == null) {
        currentTransition = runStep(currentStep, currentData)
      }

      val nextStep = graph.move(currentStep, currentTransition)

      if (nextStep == null) {
        if (currentTransition != FINISH_TRANSITION) {
          thisLogger().error("No next step for ${step.stepId} with transition $currentTransition")
        }
        return
      }

      val nextData = nextStep.typed().performStep(project, (animationData ?: return))
      if (nextData == null) {
        currentStep = nextStep
        currentTransition = STEP_UNAVAILABLE_TRANSITION
        continue
      }

      val transitionAnimation = TransitionAnimator.animateTransition(project, (animationData ?: return), currentData, nextData)
      currentTransition = if (transitionAnimation == null) {
        null
      }
      else {
        executeAnimation(transitionAnimation, nextData)
      }

      currentStep = nextStep
      currentData = nextData
    }
  }

  private suspend fun runStep(step: ZhabaStepBase, data: ZhabaData): String {
    return Disposer.newDisposable(this).use { stepDisposable ->
      if (data is ZhabaDataWithComponent) {
        thisLogger().info("Installing ZhabaComponent for regular step ${data.zhaba.hashCode()}")
        installComponent(data.zhaba, data, stepDisposable)
      }

      val typedStep = step.typed()
      val graphData = graph.additionalStepData(typedStep)
      typedStep.executeStep(data, graphData, cs, stepDisposable)
    }
  }

  private suspend fun executeAnimation(animation: EduUiOnboardingAnimation, nextData: ZhabaData): String? {
    val zhaba = ZhabaComponent(project)
    zhaba.animation = animation

    Disposer.newDisposable(this).use { transitionDisposable ->
      thisLogger().info("Installing ZhabaComponent for animation ${zhaba.hashCode()}")
      installComponent(zhaba, nextData, transitionDisposable)
      val success = zhaba.start(cs)
      return if (success) null else RERUN_TRANSITION
    }
  }

  private fun installComponent(zhaba: ZhabaComponent, data: ZhabaData, disposable: Disposable) {
    val frame = WindowManager.getInstance().getFrame(project) ?: return
    Disposer.register(disposable, zhaba)
    frame.layeredPane.add(zhaba, JLayeredPane.PALETTE_LAYER, -1)
    zhaba.setBounds(0, 0, frame.width, frame.height)

    if (data is ZhabaDataWithComponent) {
      zhaba.trackComponent(data.component)
    }

    currentZhabaComponent = zhaba
  }

  /**
   * The Toad is interrupted if the tracked component is moved or resized or its visibility changed.
   */
  private fun ZhabaComponent.trackComponent(component: Component) {
    val componentListener = object : ComponentAdapter() {
      override fun componentHidden(e: ComponentEvent?) { stop() }
      override fun componentShown(e: ComponentEvent?) { stop() }
      override fun componentMoved(e: ComponentEvent?) { stop() }
      override fun componentResized(e: ComponentEvent?) { stop() }
    }
    val hierarchyListener = HierarchyListener { stop() }
    val hierarchyBoundsListener = object : HierarchyBoundsAdapter() {
      override fun ancestorMoved(e: HierarchyEvent?) { stop() }
      override fun ancestorResized(e: HierarchyEvent?) { stop() }
    }

    Disposer.register(this) {
      component.removeComponentListener(componentListener)
      component.removeHierarchyListener(hierarchyListener)
      component.removeHierarchyBoundsListener(hierarchyBoundsListener)
    }
    component.addComponentListener(componentListener)
    component.addHierarchyListener(hierarchyListener)
    component.addHierarchyBoundsListener(hierarchyBoundsListener)
  }
}