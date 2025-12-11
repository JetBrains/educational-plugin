package com.jetbrains.edu.uiOnboarding.testgraph

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaGraph
import kotlinx.coroutines.test.runTest

abstract class ZhabaExecutionTestCase : EduTestCase() {

  override fun runInDispatchThread(): Boolean = false

  protected fun zhabaScenario(graph: ZhabaGraph, firstStepId: String, init: ZhabaScenario.() -> Unit) = runTest {
    val zhabaScenario = ZhabaScenario(project, graph, firstStepId, this, testRootDisposable)
    zhabaScenario.start(init)
  }
}