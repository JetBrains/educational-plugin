// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowId
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.swing.JComponent
import javax.swing.JLayeredPane

// copy-pasted from mono-repo
class EduUiOnboardingExecutor(private val project: Project,
                                       private val steps: List<Pair<String, EduUiOnboardingStep>>,
                                       private val cs: CoroutineScope,
                                       parentDisposable: Disposable) {
  private val disposable = Disposer.newCheckedDisposable()

  private var curStepId: String? = null
  private var curStepStartMillis: Long? = null

  init {
    Disposer.register(parentDisposable, disposable)
  }

  suspend fun start() {
    runStep(0)
  }

  private suspend fun runStep(ind: Int) {
    if (ind >= steps.size) {
      return
    }

    val (stepId, step) = steps[ind]
    val stepDisposable = Disposer.newCheckedDisposable()
    Disposer.register(disposable, stepDisposable)

    val stepStartMillis = System.currentTimeMillis()
    curStepId = stepId
    curStepStartMillis = stepStartMillis


    val gotItData = step.performStep(project, stepDisposable)
    if (gotItData == null) {
      runStep(ind + 1)
      return
    }

    val showInCenter = gotItData.position == null
    val builder = gotItData.builder
    builder.withStepNumber("${ind + 1}/${steps.size}")
      .onEscapePressed {
        finishOnboarding()
      }
      .requestFocus(true)

    if (ind < steps.lastIndex) {
      builder.withButtonLabel(EduUiOnboardingBundle.message("gotIt.button.next"))
        .onButtonClick {
          Disposer.dispose(stepDisposable)
          cs.launch(Dispatchers.EDT) {
            runStep(ind + 1)
          }
        }
        .withSecondaryButton(EduUiOnboardingBundle.message("gotIt.button.skipAll")) {
          finishOnboarding()
        }
    }
    else {
      builder.withButtonLabel(EduUiOnboardingBundle.message("gotIt.button.finish"))
        .withContrastButton(true)
        .onButtonClick {
          finishOnboarding()
        }
        .withSecondaryButton(EduUiOnboardingBundle.message("gotIt.button.restart")) {
          Disposer.dispose(stepDisposable)
          cs.launch(Dispatchers.EDT) {
            runStep(0)
          }
        }
    }

    val balloon = builder.build(stepDisposable) {
      // do not show the pointer if the balloon should be centered
      setShowCallout(!showInCenter)
    }

    showZhaba(gotItData.zhaba)

    if (showInCenter) {
      balloon.showInCenterOf(gotItData.relativePoint.originalComponent as JComponent)
    }
    else {
      balloon.show(gotItData.relativePoint, gotItData.position)
    }
  }

  private fun showZhaba(zhaba: ZhabaComponent) {
    val frame = WindowManager.getInstance().getFrame(project)!!
    frame.layeredPane.add(zhaba, JLayeredPane.PALETTE_LAYER)
    zhaba.setBounds(0, 0, frame.width, frame.height)
    zhaba.repaint()
  }

  private fun finishOnboarding() {
    Disposer.dispose(disposable)
    EduUiOnboardingService.getInstance(project).onboardingFinished()
    NotificationGroupManager.getInstance()
      .getNotificationGroup("EduOnboarding")
      .createNotification(EduUiOnboardingBundle.message("finished.reminder"), MessageType.INFO)
      .notify(project)

    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MEET_NEW_UI)
    toolWindow?.activate(null)
  }
}