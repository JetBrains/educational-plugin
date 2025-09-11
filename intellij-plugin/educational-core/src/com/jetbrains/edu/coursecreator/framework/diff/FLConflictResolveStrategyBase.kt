package com.jetbrains.edu.coursecreator.framework.diff

import com.jetbrains.edu.coursecreator.framework.FLTaskStateCC

abstract class FLConflictResolveStrategyBase : FLConflictResolveStrategy {
  /**
   * Resolve some simple changes
   *
   * If at least two versions of the file are equal, then we can resolve the change automatically.
   * Otherwise, the strategy counts this file as conflict
   */
  protected fun resolveSimpleConflicts(
    currentState: FLTaskStateCC,
    baseState: FLTaskStateCC,
    targetState: FLTaskStateCC,
  ): FLConflictResolveStrategy.StateWithResolvedChanges {
    val allFiles = currentState.keys + baseState.keys + targetState.keys

    val resultState = mutableMapOf<String, String>()
    val conflictFiles = mutableListOf<String>()

    for (file in allFiles) {
      val (isConflict, text) = resolveForFile(currentState[file], baseState[file], targetState[file])
      if (isConflict) {
        conflictFiles.add(file)
      }
      if (text != null) {
        resultState[file] = text
      }
    }
    return FLConflictResolveStrategy.StateWithResolvedChanges(conflictFiles, resultState)
  }

  // returns (isConflictFile, content)
  private fun resolveForFile(contentLeft: String?, contentBase: String?, contentRight: String?): Pair<Boolean, String?> {
    return when {
      contentBase == null && contentLeft == null && contentRight == null -> error("This branch should not be reachable")

      // User didn't make any changes with this file in the current task, so there are no changes to propagate.
      // Select a content from targetTask (contentRight)
      contentBase == contentLeft -> false to contentRight

      // User didn't make any changes with this file in the target task, so we can propagate the changes
      // Select a content from currentTask (contentLeft)
      contentBase == contentRight -> false to contentLeft

      // User changed this file in currentTask and targetTask, and the resulting versions are equal
      // Changes between (contentLeft, contentBase) and (contentRight, contentBase) are the same, so we can use any of them
      // Select a content from currentTask (contentLeft)
      contentLeft == contentRight -> false to contentLeft

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
    currentState: FLTaskStateCC,
    baseState: FLTaskStateCC,
    targetState: FLTaskStateCC
  ): FLConflictResolveStrategy.StateWithResolvedChanges {
    return resolveSimpleConflicts(currentState, baseState, targetState)
  }
}