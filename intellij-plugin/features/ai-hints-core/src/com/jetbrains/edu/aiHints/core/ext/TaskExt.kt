package com.jetbrains.edu.aiHints.core.ext

import com.jetbrains.edu.aiHints.core.context.AuthorSolutionContext
import com.jetbrains.edu.aiHints.core.context.TaskHintsDataHolder.Companion.getInstance
import com.jetbrains.edu.aiHints.core.context.TaskHintsDataHolder.TaskHintData
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task

var Task.hintData: TaskHintData
  get() {
    val project = project ?: error("No project for task $name")
    return getInstance(project).getOrCreate(project, this)
  }
  set(value) {
    val project = project ?: error("No project for task $name")
    getInstance(project).data[this] = value
  }

/**
 * Stores a context created by the author's solution, if any.
 */
var Task.authorSolutionContext: AuthorSolutionContext
  get() = hintData.authorSolutionContext
  set(value) {
    val currentHintData = hintData
    hintData = TaskHintData(
      value,
      currentHintData.taskFilesWithChangedFunctions
    )
  }

/**
 * Stores a map of a [TaskFile] full names (including paths) to functions that can be changed.
 * This map stores only task files in which changes have been made in the author's solution.
 */
var Task.taskFilesWithChangedFunctions: Map<String, List<String>>?
  get() = hintData.taskFilesWithChangedFunctions
  set(value) {
    val currentHintData = hintData
    hintData = TaskHintData(
      currentHintData.authorSolutionContext,
      value
    )
  }
