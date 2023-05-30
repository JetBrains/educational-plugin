package com.jetbrains.edu.learning.checker

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckResult
import com.jetbrains.edu.learning.courseFormat.tasks.Task

interface CheckListener {
  fun beforeCheck(project: Project, task: Task) {}
  fun afterCheck(project: Project, task: Task, result: CheckResult) {}

  companion object {
    val EP_NAME: ExtensionPointName<CheckListener> = ExtensionPointName.create("Educational.checkListener")
  }
}
