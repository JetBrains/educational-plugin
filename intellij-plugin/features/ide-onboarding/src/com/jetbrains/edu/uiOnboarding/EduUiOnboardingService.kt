// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

// copy-pasted from mono-repo
@Service(Service.Level.PROJECT)
internal class EduUiOnboardingService(private val project: Project, private val cs: CoroutineScope) {

  private val myTourInProgress = AtomicBoolean(false)
  val tourInProgress: Boolean
    get() = myTourInProgress.get()

  fun startOnboarding() {
    myTourInProgress.set(true)
    val steps = getSteps()
    val executor = EduUiOnboardingExecutor(project, steps, cs, project)
    cs.launch(Dispatchers.EDT) { executor.start() }
  }

  fun onboardingFinished() {
    myTourInProgress.set(false)
  }

  private fun getSteps(): List<Pair<String, EduUiOnboardingStep>> {
    val stepIds = getDefaultStepsOrder()
    return stepIds.mapNotNull { id ->
      val step = EduUiOnboardingStep.getIfAvailable(id)
      if (step != null) id to step else null
    }
  }

  private fun getDefaultStepsOrder(): List<String> {
    return listOf("welcome", "courseView", "taskDescription", "codeEditor", "checkSolution")
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): EduUiOnboardingService = project.service()
  }
}
