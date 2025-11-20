// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.use
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaMainGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
internal class EduUiOnboardingService(private val project: Project, private val cs: CoroutineScope) : Disposable {

  private val myTourInProgress = AtomicBoolean(false)
  private val currentExecutorRef = AtomicReference<ZhabaExecutor?>(null)

  private val zhabaGraph = ZhabaMainGraph.create()

  val tourInProgress: Boolean
    get() = myTourInProgress.get()

  /**
   * Returns the currently running ZhabaExecutor, or null if no tour is running.
   * Thread-safe.
   */
  val currentExecutor: ZhabaExecutor?
    get() = currentExecutorRef.get()

  fun startOnboarding() {
    executeZhaba(ZhabaMainGraph.STEP_ID_START_ONBOARDING)
  }

  fun promoteStudentPack() {
    executeZhaba(ZhabaMainGraph.STEP_ID_START_PROMOTE_STUDENT_PACK)
  }

  private fun executeZhaba(stepIdIfInProgress: String, stepIdIfNotInProgress: String) {
    val alreadyInProgress = myTourInProgress.getAndSet(true)
    if (alreadyInProgress) {
      executeZhaba(stepIdIfInProgress)
    }
    else {
      //currentExecutor?.moveToStep(stepIdIfInProgress)
    }
  }

  /**
   * Starts a new onboarding tour only if not already in progress.
   * Ensures currentExecutor is available while running.
   */
  private fun executeZhaba(stepId: String) {
    cs.launch(Dispatchers.EDT) {
      val executor = ZhabaExecutor(project, zhabaGraph, cs, parentDisposable = this@EduUiOnboardingService)
      currentExecutorRef.set(executor)
      try {
        Disposer.register(executor) {
          onboardingFinished()
        }
        val initialStep = zhabaGraph.findStep(stepId) ?: error("Failed to move Tode to the step with id $stepId: the step not found")
        executor.use { it.start(initialStep) }
      } finally {
        // Make sure to clear the executor no matter how the coroutine finishes
        currentExecutorRef.set(null)
      }
    }
  }

  fun onboardingFinished() {
    myTourInProgress.set(false)
    currentExecutorRef.set(null)
  }

  override fun dispose() {
    onboardingFinished()
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): EduUiOnboardingService = project.service()
  }
}