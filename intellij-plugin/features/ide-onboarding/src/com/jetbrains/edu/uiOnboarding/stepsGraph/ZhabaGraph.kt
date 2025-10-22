package com.jetbrains.edu.uiOnboarding.stepsGraph

import com.jetbrains.edu.uiOnboarding.EduUiOnboardingStepGraphData
import com.jetbrains.edu.uiOnboarding.steps.*
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.HAPPY_FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.NEXT_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.RERUN_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.SAD_FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.STEP_UNAVAILABLE_TRANSITION

/**
 * Represents a graph of [ZhabaStep]s.
 */
interface ZhabaGraph {
  /**
   * Determines the next step given the current step [step] (a vertex) and the transition [transition] (the name of the edge).
   */
  fun move(step: ZhabaStepBase, transition: String): ZhabaStepBase?

  /**
   * Each [ZhabaStep] must have some additional data associated with it, see [ZhabaStep] for the information about this data.
   */
  fun <GD: GraphData> additionalStepData(step: ZhabaStep<*, GD>): GD
}

/**
 * The implementation of the [ZhabaGraph] for the main onboarding tour.
 */
class ZhabaMainGraph private constructor(
  private val edges: MutableList<Edge> = mutableListOf(),
  private val stepsData: MutableMap<ZhabaStepBase, GraphData> = mutableMapOf(),

  val initialStep: ZhabaStepBase = StartStep()
): ZhabaGraph {

  init {
    stepsData[initialStep] = GraphData.EMPTY
    val firstOnboardingStep = fillOnboardingGraph()
    edges.add(Edge(initialStep, NEXT_TRANSITION, firstOnboardingStep))
  }

  private data class Edge(
    val fromStep: ZhabaStepBase,
    val transition: String,
    val toStep: ZhabaStepBase
  )
  
  override fun move(step: ZhabaStepBase, transition: String): ZhabaStepBase? {
    val edge = edges.firstOrNull { it.fromStep == step && it.transition == transition } ?: return null
    return edge.toStep
  }

  override fun <GD: GraphData> additionalStepData(step: ZhabaStep<*, GD>): GD {
    @Suppress("UNCHECKED_CAST")
    return stepsData[step] as? GD ?: error("No data for step $step")
  }

  private fun fillOnboardingGraph(): ZhabaStepBase {
    val uiOnboardingStepsIds = getDefaultOnboardingStepsOrder()

    val stepCount = uiOnboardingStepsIds.size - 1
    if (stepCount <= 0) error("Not enough onboarding steps found")

    // create the loop 0 -> 1 -> 2 -> ... -> stepCount -> 1 with the NEXT_TRANSITION transition

    val happyEnding = HappyFinishStep()
    val sadEnding = SadFinishStep()

    stepsData[happyEnding] = GraphData.EMPTY
    stepsData[sadEnding] = GraphData.EMPTY

    val uiOnboardingSteps = uiOnboardingStepsIds.map { EduUiOnboardingStepAsZhabaStep(it) }
    val firstOnboardingStep = uiOnboardingSteps.first()

    for (i in 0..stepCount) {
      val step = uiOnboardingSteps[i]
      stepsData[step] = EduUiOnboardingStepGraphData(i == stepCount, step.stepId, i, stepCount)

      val iNext = if (i == stepCount) 1 else i + 1
      val nextStep = uiOnboardingSteps[iNext]

      edges.add(Edge(step, NEXT_TRANSITION, nextStep))
      if (i < stepCount) {
        edges.add(Edge(step, STEP_UNAVAILABLE_TRANSITION, nextStep))
      }

      edges.add(Edge(step, RERUN_TRANSITION, step))
      edges.add(Edge(step, HAPPY_FINISH_TRANSITION, happyEnding))
      edges.add(Edge(step, SAD_FINISH_TRANSITION, sadEnding))
    }

    return firstOnboardingStep
  }

  companion object {

    fun create(): ZhabaMainGraph = ZhabaMainGraph()

    private fun getDefaultOnboardingStepsOrder(): List<String> {
      return listOf(
        WelcomeStep.STEP_KEY,
        TaskDescriptionStep.STEP_KEY,
        CodeEditorStep.STEP_KEY,
        CheckSolutionStep.STEP_KEY,
        CourseViewStep.STEP_KEY
      )
    }
  }
}