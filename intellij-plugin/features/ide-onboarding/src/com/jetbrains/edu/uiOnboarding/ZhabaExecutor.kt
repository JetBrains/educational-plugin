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
import kotlinx.coroutines.CoroutineScope
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
    val animationData = this.animationData ?: return

    var currentStep = step
    var currentData = step.typed().performStep(project, animationData) ?: return

    while (true) {
      val (nextStep, nextData) = runStep(currentStep, currentData) ?: return

      currentStep = nextStep
      currentData = nextData
    }
  }

  suspend fun runStep(step: ZhabaStepBase, data: ZhabaData): Pair<ZhabaStepBase, ZhabaData>? {
    val transition = Disposer.newDisposable(this).use { stepDisposable ->
      if (data is ZhabaDataWithComponent) {
        installComponent(data.zhaba, stepDisposable)
      }

      val typedStep = step.typed()
      val graphData = graph.additionalStepData(typedStep)
      typedStep.executeStep(data, graphData, cs, stepDisposable)
    }

    var nextStep = graph.move(step, transition)

    while (true) {
      if (nextStep == null) {
        if (transition != FINISH_TRANSITION) {
          thisLogger().error("No next step for ${step.stepId} with transition $transition")
        }
        return null
      }

      val dataForNextStep = dataForNextStep(data, nextStep)
      if (dataForNextStep != null) {
        return nextStep to dataForNextStep
      }
      nextStep = graph.move(nextStep, ZhabaStep.STEP_UNAVAILABLE_TRANSITION) ?: return null
    }
  }

  private suspend fun dataForNextStep(data: ZhabaData, nextStep: ZhabaStepBase): ZhabaData? {
    var currentData = data

    while (true) {
      val animationData = this.animationData ?: return null
      val nextData = nextStep.typed().performStep(project, animationData) ?: return null

      val transitionAnimation = TransitionAnimator.animateTransition(project, animationData, currentData, nextData)
      val transitionAnimationCompleted = if (transitionAnimation == null) {
        true
      }
      else {
        executeAnimation(transitionAnimation)
      }

      if (transitionAnimationCompleted) return nextData

      currentData = nextData
    }
  }

  suspend fun executeAnimation(animation: EduUiOnboardingAnimation): Boolean {
    val zhaba = ZhabaComponent(project)
    zhaba.animation = animation

    Disposer.newDisposable(this).use { transitionDisposable ->
      installComponent(zhaba, transitionDisposable)
      return zhaba.start()
    }
  }

  fun installComponent(zhaba: ZhabaComponent, disposable: Disposable) {
    val frame = WindowManager.getInstance().getFrame(project) ?: return
    Disposer.register(disposable, zhaba)
    frame.layeredPane.add(zhaba, JLayeredPane.PALETTE_LAYER, -1)
    zhaba.setBounds(0, 0, frame.width, frame.height)

    currentZhabaComponent = zhaba
  }
}