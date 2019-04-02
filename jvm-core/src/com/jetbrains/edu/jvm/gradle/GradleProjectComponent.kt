package com.jetbrains.edu.jvm.gradle

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.jvm.gradle.generation.EduGradleUtils
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduUtils.*
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
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
        setupGradleProject(course)
      }
    }
  }

  private fun setupGradleProject(course: Course) {
    val configurator = course.configurator
    if (configurator == null) {
      LOG.warn(String.format("Failed to refresh gradle project: configurator for `%s` is null", course.languageID))
      return
    }

    if (project.getUserData(CourseProjectGenerator.EDU_PROJECT_CREATED) === java.lang.Boolean.TRUE) {
      configurator.courseBuilder.refreshProject(project)
    }
    else if (isAndroidStudio()) {
      // Unexpectedly, Android Studio corrupts content root paths after course project reopening
      // And project structure can't show project tree because of it.
      // We don't know better and cleaner way how to fix it than to refresh project.
      configurator.courseBuilder.refreshProject(project, object : EduCourseBuilder.ProjectRefreshListener {
        override fun onSuccess() {
          // We have to open current opened file in project view manually
          // because it can't restore previous state.
          val files = FileEditorManager.getInstance(project).selectedFiles
          for (file in files) {
            val task = getTaskForFile(project, file)
            if (task != null) {
              ProjectView.getInstance(project).select(file, file, false)
            }
          }
        }

        override fun onFailure(errorMessage: String) {
          LOG.warn("Failed to refresh gradle project: $errorMessage")
        }
      })
    }

    // Android Studio creates `gradlew` not via VFS so we have to refresh project dir
    VfsUtil.markDirtyAndRefresh(false, true, true, project.courseDir)
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
