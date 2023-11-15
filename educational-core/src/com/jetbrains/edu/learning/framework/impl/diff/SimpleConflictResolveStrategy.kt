package com.jetbrains.edu.learning.framework.impl.diff

import com.jetbrains.edu.learning.framework.impl.Change
import com.jetbrains.edu.learning.framework.impl.State
import com.jetbrains.edu.learning.framework.impl.UserChanges

// Will not resolve conflicts automatically, shows all the differences in the files
class SimpleConflictResolveStrategy: FLConflictResolveStrategy {
  override fun resolveConflicts(
    currentTaskState: State,
    baseState: State,
    targetTaskState: State,
  ): FLConflictResolveStrategy.Result {
    val changes = mutableListOf<Change>()
    val existingFiles = baseState.keys.toMutableSet()

    fun addNewFilesFromState(state: State) {
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