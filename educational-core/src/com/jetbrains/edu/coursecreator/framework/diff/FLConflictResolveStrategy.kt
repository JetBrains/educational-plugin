package com.jetbrains.edu.coursecreator.framework.diff

import com.jetbrains.edu.learning.framework.impl.UserChanges

interface FLConflictResolveStrategy {
  fun resolveConflicts(
    currentTaskState: Map<String, String>,
    baseState: Map<String, String>,
    targetTaskState: Map<String, String>
  ): Result

  data class Result(val areAllConflictsResolved: Boolean, val changes: UserChanges)
}