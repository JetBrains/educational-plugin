package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.waitForSmartMode
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils.setupGradleProject
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils.updateGradleSettings
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.StudyTaskManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GradleProjectActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    if (project.isDisposed || !project.isEduProject()) {
      return
    }
    if (EduGradleUtils.isConfiguredWithGradle(project)) {
      withContext(Dispatchers.EDT) {
        updateGradleSettings(project)
      }
    }

    project.waitForSmartMode()

    if (StudyTaskManager.getInstance(project).course == null) {
      LOG.warn("Opened project is with null course")
      return
    }

    if (EduGradleUtils.isConfiguredWithGradle(project)) {
      setupGradleProject(project)
    }
  }

  companion object {
    private val LOG = Logger.getInstance(GradleProjectActivity::class.java)
  }
}
