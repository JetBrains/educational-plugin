package com.jetbrains.edu.jvm.environment

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.InstallationResult

object JdkLanguageEnvironmentNoOp : JdkLanguageEnvironment {
  override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult {
    // do nothing
    return InstallationResult.Installed
  }
}