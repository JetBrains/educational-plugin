package com.jetbrains.edu.uiOnboarding.stepsGraph

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.ZhabaComponent
import kotlinx.coroutines.CoroutineScope

/**
 * This interface is the same as [ZhabaStep] but without any generic type parameters.
 * It is necessary to avoid typing issues, because the Kotlin's type system does not work well with
 * the structures like graphs with vertices of generic types `Vertex<T>`.
 * For example, one should implement edges to be of type `Edge<Vertex<*>, Vertex<*>>`, and `*` are very poorly supported.
 */
sealed interface ZhabaStepBase {
  val stepId: String

  /**
   * A typing trick in places where we need to use [ZhabaStep] with generic types.
   * Actual type-parameters are subtypes of [ZhabaData] and [GraphData], so the cast is formally incorrect,
   * but it makes the compiler happy.
   */
  @Suppress("UNCHECKED_CAST")
  fun typed(): ZhabaStep<ZhabaData, GraphData> = this as ZhabaStep<ZhabaData, GraphData>
}

/**
 * Corresponds to vertices in the [ZhabaGraph],and represents a single step of the Zhaba tour.
 * During a step, Zhaba normally stays at some position and displays a message.
 * Zhaba is normally animated during the step, for example, rotates their eyes.
 *
 * [StepData] contains information about Zhaba's position and the position of its message.
 * It depends on the size of the IDE frame, on component positions.
 * Such data should be evaluated each time we are going to perform the step.
 * It is evaluated in the [performStep] method.
 *
 * [GraphData] contains information about the graph structure, it is available only when the step is put inside the graph.
 * For example, such data contains information about the index of the step in the sequence of steps.
 */
interface ZhabaStep<StepData : ZhabaData, StepGraphData: GraphData>: ZhabaStepBase {
  /**
   * Computes the data needed for the step.
   * The UI components are searched in the IDE frame, the [ZhabaComponent] is created and the animation is created.
   *
   * This method may be called several times before Zhaba actually moves to this step, because it is called to determine
   * **where** Zhaba should move to. And during its movement, the position of components may change, and it is necessary
   * to recompute the target position of Zhaba.
   */
  fun performStep(project: Project, data: EduUiOnboardingAnimationData): StepData?

  /**
   * Make Zhaba perform the step.
   * The animation is started, the UI listeners are installed.
   * And when a user interacts with Zhaba, the transition string is returned.
   * The string is used to determine the next step.
   *
   * Some steps may not wait for the user interaction and return the transition immediately.
   */
  suspend fun executeStep(stepData: StepData, graphData: StepGraphData, cs: CoroutineScope, disposable: Disposable): String

  companion object {
    const val NEXT_TRANSITION: String = "next"
    const val SAD_FINISH_TRANSITION: String = "unhappy finish"
    const val HAPPY_FINISH_TRANSITION: String = "happy finish"

    const val RERUN_TRANSITION = "rerun"
    const val STEP_UNAVAILABLE_TRANSITION = "step_unavailable"

    /**
     * [FINISH_TRANSITION] is used to mark the last step of the onboarding flow.
     * No edges should be added with the transition of this type.
     * Thus [ZhabaGraph.move] should return null for the [FINISH_TRANSITION].
     * If [ZhabaGraph.move] returns null for some other transition, it is a signal of a graph error.
     */
    const val FINISH_TRANSITION = "finish"

    const val STEP_ID_PREFIX_TO_SPECIFIC_STEP = ".move.to."

    fun transitionToSpecificStep(targetStepId: String): String {
      return "$STEP_ID_PREFIX_TO_SPECIFIC_STEP$targetStepId"
    }

    fun parseTransitionToSpecificStep(transition: String): String? =
      if (transition.startsWith(STEP_ID_PREFIX_TO_SPECIFIC_STEP)) {
        transition.removePrefix(STEP_ID_PREFIX_TO_SPECIFIC_STEP)
      }
      else {
        null
      }
  }
}