// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.use
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaGraph
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaMainGraph
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStepBase
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
    val graph = ZhabaMainGraph.create()
    executeZhaba(graph, graph.initialStep)
  }

  private fun executeZhaba(graph: ZhabaGraph, initialStep: ZhabaStepBase) {
    val alreadyInProgress = myTourInProgress.getAndSet(true)
    if (alreadyInProgress) return

    cs.launch(Dispatchers.EDT) {
      val executor = ZhabaExecutor(project, graph, cs, parentDisposable = this@EduUiOnboardingService)
      Disposer.register(executor) {
        onboardingFinished()
      }
      executor.use { it.start(initialStep) }
    }
  }

  fun onboardingFinished() {
    myTourInProgress.set(false)
  }

  override fun dispose() {}

  companion object {
    @JvmStatic
    fun getInstance(project: Project): EduUiOnboardingService = project.service()
  }
}
