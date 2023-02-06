package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.project.Project
import com.intellij.openapi.module.Module

abstract class PlatformCourseProjectGenerator {
  protected open suspend fun prepareToOpen(project: Project, module: Module) {}
}
