package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.stepsGraph.GraphData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.NEXT_TRANSITION
import kotlinx.coroutines.CoroutineScope

object StartStepData : ZhabaData
object SadFinishData : ZhabaData
object HappyFinishData : ZhabaData

/**
 * This step does not have any actions, it always yields the transition [outboundTransition], and when the step is
 * performed, it always returns the constant [constantData].
 *
 * Such a step is intended mostly to be the initial or the final step of the step graph.
 */
abstract class NoOpStep<StepData: ZhabaData>(
  override val stepId: String,
  private val constantData: StepData,
  private val outboundTransition: String
): ZhabaStep<StepData, GraphData.EMPTY> {

  override fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData
  ): StepData = constantData

  override suspend fun executeStep(stepData: StepData, graphData: GraphData.EMPTY, cs: CoroutineScope, disposable: Disposable): String =
    outboundTransition
}

class StartStep : NoOpStep<StartStepData>(".start", StartStepData, NEXT_TRANSITION)
class SadFinishStep : NoOpStep<SadFinishData>(".sad.finish", SadFinishData, FINISH_TRANSITION)
class HappyFinishStep : NoOpStep<HappyFinishData>(".happy.finish", HappyFinishData, FINISH_TRANSITION)
