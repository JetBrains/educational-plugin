// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.use
import com.jetbrains.edu.uiOnboarding.steps.StudentPackPromotionStep
import com.jetbrains.edu.uiOnboarding.steps.ZhabaStepFactory
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaMainGraph
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.transitionToSpecificStep
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
internal class EduUiOnboardingService(private val project: Project, private val cs: CoroutineScope) : Disposable {

  private val currentExecutor = AtomicReference<ZhabaExecutor?>(null)

  val tourInProgress: Boolean
    get() = currentExecutor.get() != null

  fun startOnboarding() = executeZhaba(
    launchStepId = ZhabaMainGraph.STEP_ID_START_ONBOARDING,
    inProgressStepId = ZhabaMainGraph.getDefaultOnboardingStepsOrder().first()
  )

  fun promoteStudentPack() = executeZhaba(
    launchStepId = ZhabaMainGraph.STEP_ID_PROMOTE_STUDENT_PACK,
    inProgressStepId = StudentPackPromotionStep.STEP_ID
  )

  fun showMenu() = executeZhaba(
    ZhabaMainGraph.STEP_ID_MAIN_MENU,
    ZhabaStepFactory.MENU_STEP_ID
  )

  fun hideTode() = executeZhaba(
    launchStepId = ZhabaMainGraph.STEP_ID_HIDE,
    inProgressStepId = ZhabaMainGraph.STEP_ID_HIDE
  )

  /**
   * @param launchStepId step from which Zhaba appears on the screen
   * @param inProgressStepId step that Zhaba should move to if it is already on the screen
   */
  private fun executeZhaba(launchStepId: String, inProgressStepId: String) {
    var newExecutorCreated = false
    val executor = currentExecutor.updateAndGet { existing ->
      if (existing == null) {
        newExecutorCreated = true
        ZhabaExecutor(project, ZhabaMainGraph.create(), cs, parentDisposable = this@EduUiOnboardingService)
      }
      else {
        existing
      }
    } ?: return

    if (!newExecutorCreated) {
      executor.interrupt(transitionToSpecificStep(inProgressStepId))
      return
    }

    // launch new executor
    cs.launch(Dispatchers.EDT) {
      Disposer.register(executor) {
        onboardingFinished()
      }

      executor.use {
        val initialStep = executor.graph.findStep(launchStepId)
        if (initialStep == null) {
          thisLogger().error("Step $launchStepId not found")
          return@launch
        }

        it.start(initialStep)
      }
    }
  }

  private fun onboardingFinished() {
    currentExecutor.set(null)
  }

  override fun dispose() {}

  companion object {
    @JvmStatic
    fun getInstance(project: Project): EduUiOnboardingService = project.service()
  }
}
