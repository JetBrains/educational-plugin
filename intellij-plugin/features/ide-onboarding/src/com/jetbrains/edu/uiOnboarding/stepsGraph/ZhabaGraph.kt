package com.jetbrains.edu.uiOnboarding.stepsGraph

import com.jetbrains.edu.uiOnboarding.steps.tour.EduUiOnboardingStepGraphData
import com.jetbrains.edu.uiOnboarding.steps.GotItBalloonGraphData
import com.jetbrains.edu.uiOnboarding.steps.ZhabaStepFactory
import com.jetbrains.edu.uiOnboarding.steps.tour.CheckSolutionStep
import com.jetbrains.edu.uiOnboarding.steps.tour.CodeEditorStep
import com.jetbrains.edu.uiOnboarding.steps.tour.CourseViewStep
import com.jetbrains.edu.uiOnboarding.steps.tour.EduUiOnboardingStepFactory
import com.jetbrains.edu.uiOnboarding.steps.tour.TaskDescriptionStep
import com.jetbrains.edu.uiOnboarding.steps.tour.TranslationStep
import com.jetbrains.edu.uiOnboarding.steps.tour.WelcomeStep
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.HAPPY_FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.NEXT_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.RERUN_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.SAD_FINISH_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.STEP_UNAVAILABLE_TRANSITION
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.parseTransitionToSpecificStep

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

  fun findStep(stepId: String): ZhabaStepBase?
}

/**
 * The implementation of the [ZhabaGraph] for the main onboarding tour.
 */
class ZhabaMainGraph private constructor(
  private val edges: MutableList<Edge> = mutableListOf(),
  private val stepsData: MutableMap<ZhabaStepBase, GraphData> = mutableMapOf(),
): ZhabaGraph {

  init {
    val initialOnboardingStep: ZhabaStepBase = ZhabaStepFactory.noOpStep(STEP_ID_START_ONBOARDING, NEXT_TRANSITION, StartOnboardingZhabaData)
    stepsData[initialOnboardingStep] = GraphData.EMPTY
    val firstOnboardingStep = fillOnboardingGraph()
    edges.add(Edge(initialOnboardingStep, NEXT_TRANSITION, firstOnboardingStep))

    val initialStudentPackPromotionStep: ZhabaStepBase = ZhabaStepFactory.noOpStep(STEP_ID_PROMOTE_STUDENT_PACK, NEXT_TRANSITION, StartStudentPackPromotionZhabaData)
    stepsData[initialStudentPackPromotionStep] = GraphData.EMPTY
    val studentPackPromotionStep = fillStudentPackPromotionGraph()
    edges.add(Edge(initialStudentPackPromotionStep, NEXT_TRANSITION, studentPackPromotionStep))

    val initialMenuStep = ZhabaStepFactory.noOpStep(STEP_ID_MAIN_MENU, NEXT_TRANSITION, StartOnboardingZhabaData)
    stepsData[initialMenuStep] = GraphData.EMPTY
    val menuStep: ZhabaStepBase = ZhabaStepFactory.menuStep()
    stepsData[menuStep] = GraphData.EMPTY
    edges.add(Edge(initialMenuStep, NEXT_TRANSITION, menuStep))

    val hideStep = ZhabaStepFactory.noOpStep(STEP_ID_HIDE, FINISH_TRANSITION) { JumpingAwayZhabaData(it.winking) }
    stepsData[hideStep] = GraphData.EMPTY
  }

  private data class Edge(
    val fromStepId: String,
    val transition: String,
    val toStepId: String
  ) {
    constructor(step: ZhabaStepBase, transition: String, toStep: ZhabaStepBase) : this(step.stepId, transition, toStep.stepId)
  }

  override fun move(step: ZhabaStepBase, transition: String): ZhabaStepBase? {
    // first, tests predefined transitions
    if (transition == RERUN_TRANSITION) return step
    val specificStep = parseTransitionToSpecificStep(transition)
    if (specificStep != null) return findStep(specificStep)

    // second, find transition in the graph
    val edge = edges.firstOrNull { it.fromStepId == step.stepId && it.transition == transition } ?: return null
    return findStep(edge.toStepId)
  }

  override fun <GD: GraphData> additionalStepData(step: ZhabaStep<*, GD>): GD {
    @Suppress("UNCHECKED_CAST")
    return stepsData[step] as? GD ?: error("No data for step $step")
  }

  override fun findStep(stepId: String): ZhabaStepBase? {
    return stepsData.keys.firstOrNull { it.stepId == stepId }
  }

  private fun fillOnboardingGraph(): ZhabaStepBase {
    val uiOnboardingStepsIds = getOrderedListOfOnboardingStepFactories()

    val stepCount = uiOnboardingStepsIds.size - 1
    if (stepCount <= 0) error("Not enough onboarding steps found")

    // create the loop 0 -> 1 -> 2 -> ... -> stepCount -> 1 with the NEXT_TRANSITION transition

    val happyEnding = ZhabaStepFactory.noOpStep(".end.happy", NEXT_TRANSITION) { JumpingAwayZhabaData(it.winking) }
    val sadEnding = ZhabaStepFactory.noOpStep(".end.sad", NEXT_TRANSITION) { JumpingAwayZhabaData(it.sad) }
    val notifyStap = ZhabaStepFactory.onboardingLastStep(".onboarding.finished")

    stepsData[happyEnding] = GraphData.EMPTY
    stepsData[sadEnding] = GraphData.EMPTY
    stepsData[notifyStap] = GraphData.EMPTY

    val uiOnboardingSteps = uiOnboardingStepsIds.map { ZhabaStepFactory.onboardingStep(it) }
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

      edges.add(Edge(step, HAPPY_FINISH_TRANSITION, happyEnding))
      edges.add(Edge(step, SAD_FINISH_TRANSITION, sadEnding))
    }
    edges.add(Edge(happyEnding, NEXT_TRANSITION, notifyStap))
    edges.add(Edge(sadEnding, NEXT_TRANSITION, notifyStap))

    return firstOnboardingStep
  }

  private fun fillStudentPackPromotionGraph(): ZhabaStepBase {
    val studentPackPromotionStep = ZhabaStepFactory.studentPackPromotionStep()
    stepsData[studentPackPromotionStep] = GotItBalloonGraphData(null, 1)

    val sadEnding = ZhabaStepFactory.noOpStep(".end.sad.scholar", FINISH_TRANSITION) { JumpingAwayZhabaData(it.scholarSad) }
    stepsData[sadEnding] = GraphData.EMPTY

    edges.add(Edge(studentPackPromotionStep, SAD_FINISH_TRANSITION, sadEnding))

    return studentPackPromotionStep
  }

  companion object {

    const val STEP_ID_MAIN_MENU = ".start.main.menu"
    const val STEP_ID_START_ONBOARDING = ".start.onboarding"
    const val STEP_ID_PROMOTE_STUDENT_PACK = ".promote.student.pack"
    const val STEP_ID_HIDE = ".hide"

    fun create(): ZhabaMainGraph = ZhabaMainGraph()

    fun getOrderedListOfOnboardingStepFactories(): List<EduUiOnboardingStepFactory> {
      return listOf(
        WelcomeStep,
        TaskDescriptionStep,
        CodeEditorStep,
        TranslationStep,
        CheckSolutionStep,
        CourseViewStep
      )
    }
  }
}