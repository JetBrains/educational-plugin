// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding.actions

import com.jetbrains.edu.uiOnboarding.EduUiOnboardingBundle
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaGraph
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaMainGraph
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStepBase

class CallTodeAction : CallTodeActionBase() {
  override val actionTitle: String get() = EduUiOnboardingBundle.message("action.Educational.Tode.CallTodeAction.text")

  override fun getGraphAndInitialStep(): Pair<ZhabaGraph, ZhabaStepBase> {
    val zhabaGraph = ZhabaMainGraph.create()
    return zhabaGraph to zhabaGraph.initialActionsMenuStep
  }

  companion object {
    const val ACTION_ID: String = "Educational.Tode.CallTodeAction"
  }
}