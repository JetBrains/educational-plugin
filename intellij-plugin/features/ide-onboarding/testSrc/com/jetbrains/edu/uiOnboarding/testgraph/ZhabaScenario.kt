package com.jetbrains.edu.uiOnboarding.testgraph

import com.intellij.diagnostic.dumpCoroutines
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.uiOnboarding.ZhabaExecutor
import com.jetbrains.edu.uiOnboarding.stepsGraph.GraphData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaGraph
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStepBase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds

class ZhabaScenario(
  private val project: Project,
  private val graph: ZhabaGraph,
  private val firstStepId: String,
  private val coroutineScope: CoroutineScope,
  private val rootDisposable: Disposable
) {

  private class StepExecution(
    val step: ZhabaStepBase,
    val data: ZhabaData,
    val graphData: GraphData,
    val nextTransition: String,
    val continuation: CancellableContinuation<String>
  )

  private var currentStepExecution: StepExecution? = null

  suspend fun start(init: ZhabaScenario.() -> Unit) {
    val spyGraph = spyk(graph)
    every { spyGraph.findStep(any()) } answers {
      callOriginal()?.typed()?.spyStep()
    }
    every { spyGraph.move(any(), any()) } answers {
      callOriginal()?.typed()?.spyStep()
    }

    val jobExecutor = coroutineScope.launch {
      withContext(Dispatchers.EDT) {
        val zhabaExecutor = ZhabaExecutor(project, spyGraph, coroutineScope, rootDisposable)

        val firstStep = spyGraph.findStep(firstStepId) ?: error("Step $firstStepId not found")
        zhabaExecutor.start(firstStep)
      }
    }

    val jobScenario = coroutineScope.launch {
      withContext(Dispatchers.EDT) {
        init()
      }
    }

    Disposer.register(rootDisposable) {
      jobExecutor.cancel()
      jobScenario.cancel()
    }

    try {
      // The same timeout as `PlatformTestUtil.MAX_WAIT_TIME`
      withContext(Dispatchers.Default.limitedParallelism(1)) {
        withTimeout(30.seconds) {
          jobScenario.join()
          jobExecutor.join()
        }
      }
    }
    catch (e: TimeoutCancellationException) {
      throw RuntimeException("Cannot wait for zhaba scenario to complete\n${dumpCoroutines()}", e)
    }
  }

  fun expectStep(stepId: String) {
    val execution = assertNotNull(currentStepExecution, "No step execution")
    assertEquals("Unexpected step", stepId, execution.step.stepId)
  }

  fun expectTransition(transition: String) {
    val execution = assertNotNull(currentStepExecution, "No step execution")
    val nextTransition = execution.nextTransition

    assertEquals("Unexpected transition", transition, nextTransition)
    execution.continuation.resume(transition) { th, value, _ ->
      fail("Waiting for transition $transition interrupted. Value: $value", th)
    }
  }

  private inline fun <reified ZD: ZhabaData, reified GD: GraphData> ZhabaStep<ZD, GD>.spyStep(): ZhabaStep<ZD, GD> {
    val spyStep = spyk(this)

    coEvery { spyStep.executeStep(any(), any(), any(), any()) } coAnswers {
      suspendCancellableCoroutine { continuation ->
        currentStepExecution = StepExecution(
          spyStep,
          firstArg(),
          secondArg(),
          callOriginal(),
          continuation
        )
      }
    }

    return spyStep
  }

}