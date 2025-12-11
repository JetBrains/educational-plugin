package com.jetbrains.edu.uiOnboarding.testgraph

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.stepsGraph.GraphData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import kotlinx.coroutines.CoroutineScope

class TestStep(
  override val stepId: String,
  internal val perform: () -> ZhabaData? = { ZhabaData.EMPTY },
  internal val execute: () -> String = { "next" }
) : ZhabaStep<ZhabaData, GraphData> {

  override fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData
  ): ZhabaData? = perform()

  override suspend fun executeStep(
    stepData: ZhabaData,
    graphData: GraphData,
    cs: CoroutineScope,
    disposable: Disposable
  ): String = execute()
}

class StepBuilderContext(private val stepId: String) {

  private var perform: () -> ZhabaData? = { ZhabaData.EMPTY }
  private var execute: () -> String = { "next" }

  fun perform(data: ZhabaData?) {
    this.perform = { data }
  }

  fun perform(perform: () -> ZhabaData?) {
    this.perform = perform
  }

  fun execute(transition: String) {
    this.execute = { transition }
  }

  fun execute(execute: () -> String) {
    this.execute = execute
  }

  fun build() = TestStep(stepId, perform, execute)
}