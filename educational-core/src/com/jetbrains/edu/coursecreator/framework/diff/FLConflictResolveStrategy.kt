package com.jetbrains.edu.coursecreator.framework.diff

import com.jetbrains.edu.learning.framework.impl.FLTaskState

interface FLConflictResolveStrategy {
  /**
   * Try automatically:
   * 1) calculate diff([baseState], [currentTaskState]) and diff([baseState], [targetTaskState])
   * 2) automatically merge simple changes
   * 3) apply them to [baseState]
   */
  fun resolveConflicts(
    currentTaskState: FLTaskState,
    baseState: FLTaskState,
    targetTaskState: FLTaskState
  ): StateWithResolvedChanges

  /**
   * Represents the state of a task with resolved changes.
   *
   * @property areAllChangesResolved Flag indicating if all changes were resolved automatically.
   *                                 If `true`, all changes were resolved automatically,
   *                                 otherwise, there are unresolved conflicts.
   * @property state The state of the task after resolving conflicts.
   */
  data class StateWithResolvedChanges(val areAllChangesResolved: Boolean, val state: FLTaskState)
}