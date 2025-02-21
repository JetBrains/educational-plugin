package com.jetbrains.edu.aiDebugging.kotlin.slice

import org.jetbrains.kotlin.idea.completion.reference
import org.jetbrains.kotlin.psi.*
import com.jetbrains.edu.aiDebugging.kotlin.slice.DependencyDirection.FORWARD

/**
 * Combines data and control dependencies for multiple Kotlin functions.
 *
 * @param maxDepth The maximum depth to recursively analyze dependencies for a function. Defaults to 3.
 *
 * @see FunctionDataDependency Calculates data dependencies for a function.
 * @see FunctionControlDependency Calculates control dependencies for a function.
 */
class CodeDependencyAnalyzer(private val maxDepth: Int = 3) {

  private val functionDependencies = mutableMapOf<KtFunction, PsiElementToDependencies>()

  private fun processDependency(function: KtFunction, depth: Int = 0, dependencyDirection: DependencyDirection) {
    if (depth >= maxDepth || function in functionDependencies) return
    val dataDependency = FunctionDataDependency(function)
    val controlDependency = FunctionControlDependency(function)

    val dataMap = if (dependencyDirection == FORWARD) dataDependency.dependenciesForward else dataDependency.dependenciesBackward
    val controlMap = if (dependencyDirection == FORWARD) controlDependency.dependenciesForward else controlDependency.dependenciesBackward

    functionDependencies[function] = (dataMap.keys + controlMap.keys).associateWith {
      ((dataMap[it] ?: emptySet()) + (controlMap[it] ?: emptySet())).toHashSet()
    }
    dataMap.keys.filterIsInstance<KtFunction>().forEach { processDependency(it, depth + 1, dependencyDirection) }
    dataMap.values.flatten().filterIsInstance<KtFunction>().forEach { processDependency(it, depth + 1, dependencyDirection) }
    controlMap.values.flatten().filterIsInstance<KtCallExpression>().forEach { element ->
      element.calleeExpression?.reference()?.resolve()?.let { (it as? KtFunction)?.let { function ->
        processDependency(function, depth + 1, dependencyDirection) }
      }
    }
  }

  /**
   * Recursively analyzes data and control dependencies starting from the given function in a specified dependency direction.
   *
   * @param function The Kotlin function (`KtFunction`) for which dependencies need to be processed.
   * @param dependencyDirection The direction of the dependency analysis, forward or backward. Defaults to forward.
   */
  fun processDependency(function: KtFunction, dependencyDirection: DependencyDirection = FORWARD): PsiElementToDependencies {
    functionDependencies.clear()
    processDependency(function, 0, dependencyDirection)
    return functionDependencies.values
      .flatMap { it.entries }
      .groupBy({ it.key }, { it.value })
      .mapValues { (_, values) -> values.flatten().toHashSet() }
  }
}
