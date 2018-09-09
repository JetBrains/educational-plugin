package com.jetbrains.edu.learning.checker.remote

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object RemoteTaskCheckerManager {
  private val EP_NAME = ExtensionPointName.create<RemoteTaskChecker>("Educational.remoteTaskChecker")

  @JvmStatic
  fun remoteCheckerForTask(project: Project, task: Task): RemoteTaskChecker? {
    val checkers = Extensions.getExtensions(EP_NAME).filter { it.canCheck(project, task) }
    if (checkers.isEmpty()) {
      return null;
    }
    if (checkers.size > 1) {
      error("Several remote task checkers available for ${task.taskType}:${task.name}: $checkers")
    }
    return checkers[0];
  }

}