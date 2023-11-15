package com.jetbrains.edu.coursecreator.framework.diff

import com.jetbrains.edu.learning.framework.impl.Change
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.framework.impl.UserChanges

// Will not resolve conflicts automatically, shows all the differences in the files
class SimpleConflictResolveStrategy : FLConflictResolveStrategy {
  override fun resolveConflicts(
    currentTaskState: FLTaskState,
    baseState: FLTaskState,
    targetTaskState: FLTaskState,
  ): FLConflictResolveStrategy.Result {
    val changes = mutableListOf<Change>()
    val existingFiles = baseState.keys.toMutableSet()

    fun addNewFilesFromState(state: FLTaskState) {
      for ((name, content) in state) {
        when {
          (name !in existingFiles) -> {
            changes += Change.AddFile(name, content)
            existingFiles += name
          }
        }
      }
    }

    addNewFilesFromState(currentTaskState)
    addNewFilesFromState(targetTaskState)

    return FLConflictResolveStrategy.Result(false, UserChanges(changes))
  }
}