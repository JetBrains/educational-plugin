// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.uiOnboarding.steps.CheckSolutionStep
import com.jetbrains.edu.uiOnboarding.steps.CodeEditorStep
import com.jetbrains.edu.uiOnboarding.steps.CourseViewStep
import com.jetbrains.edu.uiOnboarding.steps.TaskDescriptionStep
import com.jetbrains.edu.uiOnboarding.steps.WelcomeStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

// copy-pasted from mono-repo
@Service(Service.Level.PROJECT)
internal class EduUiOnboardingService(private val project: Project, private val cs: CoroutineScope) : Disposable {

  private val myTourInProgress = AtomicBoolean(false)
  val tourInProgress: Boolean
    get() = myTourInProgress.get()

  fun startOnboarding() {
    val alreadyInProgress = myTourInProgress.getAndSet(true)
    if (alreadyInProgress) return

    val steps = getSteps()
    val data = EduUiOnboardingAnimationData.load() ?: return
    val executor = EduUiOnboardingExecutor(project, data, steps, cs, this)
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
    return listOf(
      WelcomeStep.STEP_KEY,
      TaskDescriptionStep.STEP_KEY,
      CodeEditorStep.STEP_KEY,
      CheckSolutionStep.STEP_KEY,
      CourseViewStep.STEP_KEY
    )
  }

  override fun dispose() {}

  companion object {
    @JvmStatic
    fun getInstance(project: Project): EduUiOnboardingService = project.service()
  }
}
