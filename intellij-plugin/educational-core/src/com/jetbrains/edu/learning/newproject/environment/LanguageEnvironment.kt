package com.jetbrains.edu.learning.newproject.environment

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.EduProjectSettings

sealed class InstallationResult {
  data object Installed : InstallationResult()
  data class Error(@NlsContexts.StatusText val message: String) : InstallationResult()
}

/**
 * Represents the environment required for the project to work.
 */
interface LanguageEnvironment : EduProjectSettings {
  /**
   * Called just after the project is created to set it up.
   */
  suspend fun installIfNeeded(project: Project, course: Course): InstallationResult

  /**
   * Some languages do not require any additional environment setup
   */
  object NoOp : LanguageEnvironment {
    override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult = InstallationResult.Installed
  }
}