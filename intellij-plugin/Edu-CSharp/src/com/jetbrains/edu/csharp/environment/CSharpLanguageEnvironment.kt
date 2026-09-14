package com.jetbrains.edu.csharp.environment

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.environment.InstallationResult
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import com.jetbrains.rider.model.MonitoringStartMode
import com.jetbrains.rider.model.dpaModel
import com.jetbrains.rider.projectView.solution
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CSharpLanguageEnvironment : LanguageEnvironment {
  override suspend fun installIfNeeded(project: Project, course: Course): InstallationResult {
    withContext(Dispatchers.EDT) {
      project.solution.dpaModel.monitoringStartMode.set(MonitoringStartMode.OnDebug)
    }
    return InstallationResult.Installed
  }
}
