package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.waitForSmartMode
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils.setupGradleProject
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils.updateGradleSettings
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GradleProjectActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    if (!project.isEduProject()) return
    if (!EduGradleUtils.isConfiguredWithGradle(project)) return

    withContext(Dispatchers.EDT) {
      updateGradleSettings(project)
    }

    project.waitForSmartMode()

    setupGradleProject(project)
  }
}
