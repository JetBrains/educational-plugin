package com.jetbrains.edu.coursecreator.framework.diff

import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.framework.impl.UserChanges

interface FLConflictResolveStrategy {
  fun resolveConflicts(
    currentTaskState: FLTaskState,
    baseState: FLTaskState,
    targetTaskState: FLTaskState
  ): Result

  data class Result(val areAllConflictsResolved: Boolean, val changes: UserChanges)
}