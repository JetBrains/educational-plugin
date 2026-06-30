package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.application.EDT
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.edu.jvm.environment.JdkBuildSystemSupport
import com.jetbrains.edu.jvm.environment.JdkVersionRange
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.settings.SbtProjectSettings

object SbtBuildSystemSupport : JdkBuildSystemSupport {
  override suspend fun configureProject(project: Project, course: Course, jdk: Sdk) {
    withContext(Dispatchers.EDT) {
      val location = project.basePath ?: error("Failed to find base path for the project during scala sbt setup")
      val systemSettings = ExternalSystemApiUtil.getSettings(project, SbtProjectSystem.Id)

      val projectSettings = SbtProjectSettings()
      projectSettings.externalProjectPath = location

      val projects = systemSettings.linkedProjectsSettings.toHashSet()
      projects.add(projectSettings)
      systemSettings.linkedProjectsSettings = projects
    }
  }

  override fun getJdkVersionRange(course: Course): Result<JdkVersionRange, String> {
    return Ok(JdkVersionRange.All)
  }
}