// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.ide.ui.UISettingsListener
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.EDT
import com.intellij.openapi.observable.util.addComponentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.UiOnboardingRelaunchLocation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.zhabaScale
import com.jetbrains.edu.uiOnboarding.transitions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JComponent
import javax.swing.JLayeredPane

// copy-pasted from mono-repo
class EduUiOnboardingExecutor(
  private val project: Project,
  private val animationData: EduUiOnboardingAnimationData,
  private val steps: List<Pair<String, EduUiOnboardingStep>>,
  private val cs: CoroutineScope,
  parentDisposable: Disposable
) {
  private val disposable = Disposer.newCheckedDisposable()

  private var curStepId: String? = null

  private var currentZhabaComponent: ZhabaComponent? = null

  init {
    Disposer.register(parentDisposable, disposable)

    // Listen to IDE resize
    WindowManager.getInstance().getFrame(project)?.addComponentListener(parentDisposable = parentDisposable, object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent?) {
        changeZhabaLocation()
      }
    })

    // Listen to IDE Zoom changes and other changes that might affect UI components positions and sizes
    project.messageBus.connect(parentDisposable).subscribe(UISettingsListener.TOPIC, UISettingsListener {
      changeZhabaLocation()
    })
  }

  suspend fun start() {
    runStep(0)
  }

  /**
   * Notify zhaba that it has to change its location because of movements of UI components.
   */
  private fun changeZhabaLocation() {
    currentZhabaComponent?.stop()
  }

  private suspend fun runStep(ind: Int, previousStepId: String? = null, previousData: EduUiOnboardingStepData? = null) {
    if (ind >= steps.size) {
      return
    }

    val stepDisposable = Disposer.newCheckedDisposable()
    Disposer.register(disposable, stepDisposable)

    fun finishStep() {
      Disposer.dispose(stepDisposable)
    }

    val (stepId, step) = steps[ind]

    curStepId = stepId

    val gotItData = step.performStep(project, animationData, stepDisposable)
    if (gotItData == null || !gotItData.tooltipPoint.component.isShowing) {
      finishStep()
      runStep(ind + 1, previousStepId, previousData)
      return
    }

    /**
     * If component positions have changes, Zhaba may sit in the wrong place.
     * Rerunning the current step makes Zhaba to find the correct position and to move there.
     */
    suspend fun rerunCurrentStep() {
      finishStep()
      runStep(ind, stepId, gotItData)
    }

    fun proceedToStep(ind: Int, previousStepId: String?, previousData: EduUiOnboardingStepData?) {
      finishStep()
      cs.launch(Dispatchers.EDT) {
        runStep(ind, previousStepId, previousData)
      }
    }

    fun proceedToFinishOnboarding(data: EduUiOnboardingStepData, isHappy: Boolean) {
      finishStep()
      cs.launch(Dispatchers.EDT) {
        finishOnboarding(data, isHappy)
      }
    }

    if (previousData != null && previousStepId != null) {
      val completed = animateTransitionBetweenSteps(previousStepId, stepId, previousData, gotItData)
      if (!completed) {
        rerunCurrentStep()
        return
      }
    }

    val showInCenter = gotItData.position == null
    val builder = gotItData.builder

    if (ind > 0) {
      builder.withStepNumber("$ind/${steps.size - 1}")
    }

    builder.onEscapePressed {
      EduCounterUsageCollector.uiOnboardingSkipped(ind, stepId)
      proceedToFinishOnboarding(gotItData, isHappy = false)
    }.requestFocus(true)

    if (ind < steps.lastIndex) {
      builder.withButtonLabel(EduUiOnboardingBundle.message("gotIt.button.next")).onButtonClick {
        if (ind == 0) {
          EduCounterUsageCollector.uiOnboardingStarted()
        }
        proceedToStep(ind + 1, curStepId, gotItData)
      }.withSecondaryButton(EduUiOnboardingBundle.message("gotIt.button.skipAll")) {
        EduCounterUsageCollector.uiOnboardingSkipped(ind, stepId)
        proceedToFinishOnboarding(gotItData, isHappy = false)
      }
    }
    else {
      builder.withButtonLabel(EduUiOnboardingBundle.message("gotIt.button.finish")).withContrastButton(true).onButtonClick {
        EduCounterUsageCollector.uiOnboardingFinished()
        proceedToFinishOnboarding(gotItData, isHappy = true)
      }.withSecondaryButton(EduUiOnboardingBundle.message("gotIt.button.restart")) {
        EduCounterUsageCollector.uiOnboardingRelaunched(UiOnboardingRelaunchLocation.TOOLTIP_RESTART_BUTTON)
        proceedToStep(1, curStepId, gotItData)
      }
    }

    val balloon = builder.build(stepDisposable) {
      // do not show the pointer if the balloon should be centered
      setShowCallout(!showInCenter)
    }

    if (showInCenter) {
      balloon.showInCenterOf(gotItData.tooltipPoint.originalComponent as JComponent)
    }
    else {
      balloon.show(gotItData.tooltipPoint, gotItData.position)
    }

    val completed = showZhaba(gotItData.zhaba)
    if (!completed) {
      rerunCurrentStep()
    }
  }

  private suspend fun showZhaba(zhaba: ZhabaComponent): Boolean {
    val frame = WindowManager.getInstance().getFrame(project)!!
    frame.layeredPane.add(zhaba, JLayeredPane.PALETTE_LAYER, -1)
    zhaba.setBounds(0, 0, frame.width, frame.height)
    currentZhabaComponent = zhaba
    return zhaba.start()
  }

  private suspend fun animateTransitionBetweenSteps(
    fromStepId: String, toStepId: String, fromData: EduUiOnboardingStepData, toData: EduUiOnboardingStepData
  ): Boolean {
    val frame = WindowManager.getInstance().getFrame(project) ?: return true

    val fromPoint = fromData.zhabaPoint
    val toPoint = toData.zhabaPoint

    val transitionAnimation = getTransition(fromStepId, toStepId, fromPoint, toPoint)

    val zhaba = ZhabaComponent(project)
    Disposer.register(disposable, zhaba)
    frame.layeredPane.add(zhaba, JLayeredPane.PALETTE_LAYER, -1)
    zhaba.setBounds(0, 0, frame.width, frame.height)
    zhaba.animation = transitionAnimation

    currentZhabaComponent = zhaba
    val success = zhaba.start()
    Disposer.dispose(zhaba)
    return success
  }

  private fun getTransition(
    fromStepId: String, toStepId: String, fromPoint: RelativePoint, toPoint: RelativePoint
  ): EduUiOnboardingAnimation? {
    if (fromStepId == toStepId) {
      return selectTransitionFromPoints(fromPoint, toPoint)
    }

    return when (fromStepId to toStepId) {
      "welcome" to "taskDescription" -> JumpRight(animationData, fromPoint, toPoint)
      "courseView" to "taskDescription" -> JumpRight(animationData, fromPoint, toPoint)
      "codeEditor" to "checkSolution" -> JumpDown(animationData, fromPoint, toPoint)
      "checkSolution" to "courseView" -> JumpLeft(animationData, fromPoint, toPoint)
      else -> null
    }
  }

  private fun selectTransitionFromPoints(fromPoint: RelativePoint, toPoint: RelativePoint): EduUiOnboardingAnimation? {
    val frame = WindowManager.getInstance().getFrame(project) ?: return null
    val localFromPoint = fromPoint.getPoint(frame)
    val localToPoint = toPoint.getPoint(frame)

    val distanceSq = localFromPoint.distanceSq(localToPoint)

    val longEnoughToJump = zhabaScale(100)

    if (distanceSq < longEnoughToJump * longEnoughToJump) {
      return ShortStep(animationData, fromPoint, toPoint, localFromPoint, localToPoint)
    }

    val longEnoughForSideJump = zhabaScale(10)
    return when {
      localFromPoint.x + longEnoughForSideJump < localToPoint.x -> JumpRight(animationData, fromPoint, toPoint)
      localFromPoint.x - longEnoughForSideJump > localToPoint.x -> JumpLeft(animationData, fromPoint, toPoint)
      localFromPoint.y < localToPoint.y -> JumpDown(animationData, fromPoint, toPoint)
      else -> JumpUp(animationData, fromPoint, toPoint)
    }
  }

  private suspend fun finishOnboarding(data: EduUiOnboardingStepData, isHappy: Boolean) {
    val frame = WindowManager.getInstance().getFrame(project)!!
    val lastZhaba = ZhabaComponent(project)
    Disposer.register(disposable, lastZhaba)
    frame.layeredPane.add(lastZhaba, JLayeredPane.PALETTE_LAYER, -1)
    lastZhaba.setBounds(0, 0, frame.width, frame.height)
    lastZhaba.animation = createLastAnimation(data, isHappy, lastZhaba)
    lastZhaba.start()

    Disposer.dispose(disposable)
    EduUiOnboardingService.getInstance(project).onboardingFinished()
    NotificationGroupManager.getInstance().getNotificationGroup("EduOnboarding")
      .createNotification(EduUiOnboardingBundle.message("finished.reminder", getMenuPath()), MessageType.INFO).notify(project)

    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MEET_NEW_UI)
    toolWindow?.activate(null)
  }

  private fun getMenuPath(): String {
    val helpAction = ActionManager.getInstance().getAction(IdeActions.GROUP_HELP_MENU)
    val zhabaAction = ActionManager.getInstance().getAction(StartEduUiOnboardingAction.ACTION_ID) as? StartEduUiOnboardingAction

    val helpName = helpAction.templatePresentation.text ?: ""
    val actionName = zhabaAction?.actionName() ?: ""

    return "$helpName > $actionName"
  }

  private fun createLastAnimation(data: EduUiOnboardingStepData, isHappy: Boolean, lastZhaba: ZhabaComponent): EduUiOnboardingAnimation {
    val fromPoint = data.zhabaPoint
    return if (isHappy) {
      HappyJumpDown(animationData, fromPoint, lastZhaba)
    }
    else {
      SadJumpDown(animationData, fromPoint, lastZhaba)
    }
  }
}