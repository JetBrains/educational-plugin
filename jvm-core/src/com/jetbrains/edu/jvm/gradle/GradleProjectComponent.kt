package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils
import com.jetbrains.edu.learning.EduUtils.isStudyProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import java.io.File

class GradleProjectComponent(private val project: Project) : ProjectComponent {

  override fun projectOpened() {
    if (project.isDisposed || !isStudyProject(project)) {
      return
    }

    StartupManager.getInstance(project).runWhenProjectIsInitialized {
      val course = StudyTaskManager.getInstance(project).course
      if (course == null) {
        LOG.warn("Opened project is with null course")
        return@runWhenProjectIsInitialized
      }

      if (EduGradleUtils.isConfiguredWithGradle(project)) {
        setupGradleProject()
      }
    }
  }

  private fun setupGradleProject() {
    val projectBasePath = project.basePath
    if (projectBasePath != null) {
      // Android Studio creates non executable `gradlew`
      val gradlew = File(FileUtil.toSystemDependentName(projectBasePath), GRADLE_WRAPPER_UNIX)
      if (gradlew.exists()) {
        gradlew.setExecutable(true)
      }
      else {
        VirtualFileManager.getInstance().addVirtualFileListener(GradleWrapperListener(project), project)
      }
    }
  }

  companion object {
    private val LOG = Logger.getInstance(GradleProjectComponent::class.java)
  }
}
