package com.jetbrains.edu.learning

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

interface TaskCustomActionProvider {

  fun getAction(project: Project): AnAction

  fun isAvailable(project: Project): Boolean = false

  companion object {
    val EP_NAME = ExtensionPointName.create<TaskCustomActionProvider>("Educational.taskCustomAction")
  }
}