// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.UiOnboardingRelaunchLocation
import com.jetbrains.edu.uiOnboarding.transitions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
  private var curStepStartMillis: Long? = null

  init {
    Disposer.register(parentDisposable, disposable)
  }

  suspend fun start() {
    runStep(0)
  }

  private suspend fun runStep(ind: Int, previousStepId: String? = null, previousData: EduUiOnboardingStepData? = null) {
    if (ind >= steps.size) {
      return
    }

    val (stepId, step) = steps[ind]
    val stepDisposable = Disposer.newCheckedDisposable()
    Disposer.register(disposable, stepDisposable)

    val stepStartMillis = System.currentTimeMillis()
    curStepId = stepId
    curStepStartMillis = stepStartMillis

    val gotItData = step.performStep(project, animationData, stepDisposable)
    if (gotItData == null || !gotItData.tooltipPoint.component.isShowing) {
      runStep(ind + 1, previousStepId, previousData)
      return
    }
    if (previousData != null && previousStepId != null) {
      animateTransitionBetweenSteps(previousStepId, stepId, previousData, gotItData)
    }

    val showInCenter = gotItData.position == null
    val builder = gotItData.builder

    if (ind > 0) {
      builder.withStepNumber("$ind/${steps.size - 1}")
    }

    builder.onEscapePressed {
      EduCounterUsageCollector.uiOnboardingSkipped(ind, stepId)
      Disposer.dispose(stepDisposable)
      cs.launch(Dispatchers.EDT) {
        finishOnboarding(gotItData, isHappy = false)
      }
    }.requestFocus(true)

    if (ind < steps.lastIndex) {
      builder.withButtonLabel(EduUiOnboardingBundle.message("gotIt.button.next")).onButtonClick {
        if (ind == 0) {
          EduCounterUsageCollector.uiOnboardingStarted()
        }
        Disposer.dispose(stepDisposable)
        cs.launch(Dispatchers.EDT) {
          runStep(ind + 1, curStepId, gotItData)
        }
      }.withSecondaryButton(EduUiOnboardingBundle.message("gotIt.button.skipAll")) {
        EduCounterUsageCollector.uiOnboardingSkipped(ind, stepId)
        Disposer.dispose(stepDisposable)
        cs.launch(Dispatchers.EDT) {
          finishOnboarding(gotItData, isHappy = false)
        }
      }
    }
    else {
      builder.withButtonLabel(EduUiOnboardingBundle.message("gotIt.button.finish")).withContrastButton(true).onButtonClick {
        EduCounterUsageCollector.uiOnboardingFinished()
        cs.launch(Dispatchers.EDT) {
          Disposer.dispose(stepDisposable)
          finishOnboarding(gotItData, isHappy = true)
        }
      }.withSecondaryButton(EduUiOnboardingBundle.message("gotIt.button.restart")) {
        EduCounterUsageCollector.uiOnboardingRelaunched(UiOnboardingRelaunchLocation.TOOLTIP_RESTART_BUTTON)
        Disposer.dispose(stepDisposable)
        cs.launch(Dispatchers.EDT) {
          runStep(1, curStepId, gotItData)
        }
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

    showZhaba(gotItData.zhaba)
  }

  private suspend fun showZhaba(zhaba: ZhabaComponent) {
    val frame = WindowManager.getInstance().getFrame(project)!!
    frame.layeredPane.add(zhaba, JLayeredPane.PALETTE_LAYER, -1)
    zhaba.setBounds(0, 0, frame.width, frame.height)
    zhaba.start()
  }

  private suspend fun animateTransitionBetweenSteps(
    fromStepId: String, toStepId: String, fromData: EduUiOnboardingStepData, toData: EduUiOnboardingStepData
  ) {
    val frame = WindowManager.getInstance().getFrame(project) ?: return

    val fromPoint = fromData.zhabaPoint
    val toPoint = toData.zhabaPoint

    val transitionAnimation = getTransition(fromStepId, toStepId, fromPoint, toPoint)

    val zhaba = ZhabaComponent(project)
    Disposer.register(disposable, zhaba)
    frame.layeredPane.add(zhaba, JLayeredPane.PALETTE_LAYER, -1)
    zhaba.setBounds(0, 0, frame.width, frame.height)
    zhaba.animation = transitionAnimation

    zhaba.start()
    Disposer.dispose(zhaba)
  }

  private fun getTransition(
    fromStepId: String, toStepId: String, fromPoint: RelativePoint, toPoint: RelativePoint
  ): EduUiOnboardingAnimation? = when (fromStepId to toStepId) {
    "welcome" to "taskDescription" -> JumpRight(animationData, fromPoint, toPoint)
    "courseView" to "taskDescription" -> JumpRight(animationData, fromPoint, toPoint)
    "codeEditor" to "checkSolution" -> JumpDown(animationData, fromPoint, toPoint)
    "checkSolution" to "courseView" -> JumpLeft(animationData, fromPoint, toPoint)
    else -> null
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