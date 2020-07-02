package com.jetbrains.edu.jvm.gradle

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.gradle.GradleConstants
import java.io.File

class GradleStartupActivity : StartupActivity.DumbAware {

  override fun runActivity(project: Project) {
    if (project.isDisposed || !EduUtils.isEduProject(project)) {
      return
    }
    if (EduGradleUtils.isConfiguredWithGradle(project)) {
      updateGradleSettings(project)
    }

    DumbService.getInstance(project).runWhenSmart {
      val taskManager = StudyTaskManager.getInstance(project)
      val course = taskManager.course
      if (course == null) {
        LOG.warn("Opened project is with null course")
        return@runWhenSmart
      }

      if (EduGradleUtils.isConfiguredWithGradle(project)) {
        setupGradleProject(project)
      }
    }
  }

  private fun updateGradleSettings(project: Project) {
    val projectBasePath = project.basePath ?: return
    val sdk = ProjectRootManager.getInstance(project).projectSdk
    EduGradleUtils.setGradleSettings(project, sdk, projectBasePath)
  }

  private fun setupGradleProject(project: Project) {
    val projectBasePath = project.basePath
    if (projectBasePath != null) {
      // Android Studio creates non executable `gradlew`
      val gradlew = File(FileUtil.toSystemDependentName(projectBasePath), GradleConstants.GRADLE_WRAPPER_UNIX)
      if (gradlew.exists()) {
        gradlew.setExecutable(true)
      }
      else {
        val taskManager = StudyTaskManager.getInstance(project)
        val connection = ApplicationManager.getApplication().messageBus.connect(taskManager)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, GradleWrapperListener(connection))
      }
    }
  }

  companion object {
    private val LOG = Logger.getInstance(GradleStartupActivity::class.java)
  }
}
