package com.jetbrains.edu.coursecreator.framework.diff

import com.jetbrains.edu.learning.framework.impl.FLTaskState

/**
 * A simple conflict resolution strategy for resolving changes between task states.
 *
 */
class SimpleConflictResolveStrategy : FLConflictResolveStrategy {
  /**
   * This strategy does not resolve changes automatically by itself
   *
   * @return [FLConflictResolveStrategy.StateWithResolvedChanges] with no resolved changes and [baseState] with added files from [currentTaskState]
   * and [targetTaskState]
   *
   * Adding new files is necessary,
   * since the user will allow changes using the merge dialog,
   * and they should be in target task as virtual files.
   */
  override fun resolveConflicts(
    currentTaskState: FLTaskState,
    baseState: FLTaskState,
    targetTaskState: FLTaskState,
  ): FLConflictResolveStrategy.StateWithResolvedChanges {
    val preparedState = baseState.toMutableMap()

    preparedState.putAllIfAbsent(currentTaskState)
    preparedState.putAllIfAbsent(targetTaskState)
    return FLConflictResolveStrategy.StateWithResolvedChanges(false, preparedState)
  }

  private fun MutableMap<String, String>.putAllIfAbsent(state: Map<String, String>) {
    for ((key, value) in state) {
      putIfAbsent(key, value)
    }
  }
}