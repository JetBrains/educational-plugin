package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.NOTIFICATIONS_SILENT_MODE

abstract class PlatformCourseProjectGenerator {
  protected open fun prepareToOpen(project: Project, module: Module) {
    NOTIFICATIONS_SILENT_MODE.set(project, true)
  }
}
