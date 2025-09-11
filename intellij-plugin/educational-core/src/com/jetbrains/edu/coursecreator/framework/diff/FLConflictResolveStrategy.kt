package com.jetbrains.edu.coursecreator.framework.diff

import com.jetbrains.edu.coursecreator.framework.FLTaskStateCC

interface FLConflictResolveStrategy {
  /**
   * Try automatically:
   * 1) calculate diff([baseState], [currentState]) and diff([baseState], [targetState])
   * 2) automatically merge simple changes
   * 3) apply them to [baseState]
   */
  fun resolveConflicts(
    currentState: FLTaskStateCC,
    baseState: FLTaskStateCC,
    targetState: FLTaskStateCC
  ): StateWithResolvedChanges

  /**
   * Represents the state of a task with resolved changes.
   *
   * @property conflictFiles Files that could not be merged automatically.
   * @property state The state of the task after resolving conflicts.
   */
  data class StateWithResolvedChanges(val conflictFiles: List<String>, val state: FLTaskStateCC)
}