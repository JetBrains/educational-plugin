package com.jetbrains.edu.coursecreator.framework.diff

import com.jetbrains.edu.learning.courseFormat.FileContents
import com.jetbrains.edu.learning.framework.impl.FLTaskState
import com.jetbrains.edu.learning.framework.impl.textRepresentationEquals

abstract class FLConflictResolveStrategyBase : FLConflictResolveStrategy {
  /**
   * Resolve some simple changes
   *
   * If at least two versions of the file are equal, then we can resolve the change automatically.
   * Otherwise, the strategy counts this file as conflict
   */
  protected fun resolveSimpleConflicts(
    currentState: FLTaskState,
    baseState: FLTaskState,
    targetState: FLTaskState,
  ): FLConflictResolveStrategy.StateWithResolvedChanges {
    val allFiles = currentState.keys + baseState.keys + targetState.keys

    val resultState = mutableMapOf<String, FileContents>()
    val conflictFiles = mutableListOf<String>()

    for (file in allFiles) {
      val (isConflict, contents) = resolveForFile(currentState[file], baseState[file], targetState[file])
      if (isConflict) {
        conflictFiles.add(file)
      }
      if (contents != null) {
        resultState[file] = contents
      }
    }
    return FLConflictResolveStrategy.StateWithResolvedChanges(conflictFiles, resultState)
  }

  // returns (isConflictFile, content)
  private fun resolveForFile(contentLeft: FileContents?, contentBase: FileContents?, contentRight: FileContents?): Pair<Boolean, FileContents?> {
    return when {
      contentBase == null && contentLeft == null && contentRight == null -> error("This branch should not be reachable")

      // User didn't make any changes with this file in the current task, so there are no changes to propagate.
      // Select a content from targetTask (contentRight)
      contentBase.textRepresentationEquals(contentLeft) -> false to contentRight

      // User didn't make any changes with this file in the target task, so we can propagate the changes
      // Select a content from currentTask (contentLeft)
      contentBase.textRepresentationEquals(contentRight) -> false to contentLeft

      // User changed this file in currentTask and targetTask, and the resulting versions are equal
      // Changes between (contentLeft, contentBase) and (contentRight, contentBase) are the same, so we can use any of them
      // Select a content from currentTask (contentLeft)
      contentLeft.textRepresentationEquals(contentRight) -> false to contentLeft

      // User added this file in currentTask and targetTask, and the resulting versions are different.
      // The conflict occurs, but there is no base version because it is equal to null.
      // To avoid showing an empty editor field in the middle of the merge dialog, we use contentLeft to initialize it
      contentBase == null -> true to contentLeft

      // User modified this file in currentTask and targetTask and the resulting versions are different
      // The conflict occurs, and we use contentBase to initialize the middle of merge dialog
      else -> true to contentBase
    }
  }
}

/**
 * A simple conflict resolution strategy for resolving changes between task states.
 */
class SimpleConflictResolveStrategy : FLConflictResolveStrategyBase() {
  override fun resolveConflicts(
    currentState: FLTaskState,
    baseState: FLTaskState,
    targetState: FLTaskState
  ): FLConflictResolveStrategy.StateWithResolvedChanges {
    return resolveSimpleConflicts(currentState, baseState, targetState)
  }
}