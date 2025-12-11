package com.jetbrains.edu.uiOnboarding.testgraph

import com.jetbrains.edu.uiOnboarding.stepsGraph.GraphData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaGraph
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStepBase

private data class Edge(
  val from: String,
  val to: String,
  val transition: String
)

private class TestZhabaGraph(
  private val steps: Map<String, TestStep> = mutableMapOf(),
  private val edges: Set<Edge> = mutableSetOf()
) : ZhabaGraph {
  override fun move(
    step: ZhabaStepBase,
    transition: String
  ): ZhabaStepBase? {
    val edge = edges.filter { it.from == step.stepId && it.transition == transition }
    if (edge.isEmpty()) return null
    if (edge.size > 1) error("More than one edge for $step")

    return steps[edge.single().to]
  }

  @Suppress("UNCHECKED_CAST")
  override fun <GD : GraphData> additionalStepData(step: ZhabaStep<*, GD>): GD = GraphData.EMPTY as GD

  override fun findStep(stepId: String): ZhabaStepBase? = steps[stepId]
}

class EdgeBuilder(val fromStep: String, val transition: String)

class TestGraphBuilderContext {

  private val steps: MutableMap<String, TestStep> = mutableMapOf()
  private val edges: MutableSet<Edge> = mutableSetOf()

  fun build(): ZhabaGraph {
    // create steps if they were not explicitly created by the `step` function
    val fromSteps = edges.map { it.from }.toSet()
    val toSteps = edges.map { it.to }.toSet()
    val stepsToCreate = fromSteps + toSteps - steps.keys
    for (step in stepsToCreate) {
      steps[step] = TestStep(step)
    }

    return TestZhabaGraph(steps, edges)
  }

  fun step(stepId: String, init: StepBuilderContext.() -> Unit): ZhabaStepBase {
    val context = StepBuilderContext(stepId)
    context.init()
    val step = context.build()
    steps[stepId] = step
    return step
  }

  infix fun ZhabaStepBase.via(transition: String): EdgeBuilder = stepId via transition
  infix fun String.via(transition: String): EdgeBuilder = EdgeBuilder(this, transition)

  infix fun EdgeBuilder.to(toStep: ZhabaStepBase): String = to(toStep.stepId)
  infix fun EdgeBuilder.to(toStep: String): String {
    edges.add(Edge(fromStep, toStep, transition))
    return toStep
  }

}

fun zhabaGraph(init: TestGraphBuilderContext.() -> Unit): ZhabaGraph {
  val context = TestGraphBuilderContext()
  context.init()
  return context.build()
}