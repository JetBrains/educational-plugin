package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtilRt
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduNames.PROJECT_NAME
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.sanitizeName
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.scala.sbt.ScalaSbtCourseBuilder.Companion.BUILD_SBT
import org.jetbrains.plugins.scala.project.Version
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.settings.SbtProjectSettings

class ScalaSbtCourseProjectGenerator(builder: ScalaSbtCourseBuilder, course: Course) : CourseProjectGenerator<JdkProjectSettings>(builder, course) {
  override fun createCourseStructure(project: Project, module: Module, baseDir: VirtualFile, settings: JdkProjectSettings) {
    GeneratorUtils.removeModule(project, module)
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)
    super.createCourseStructure(project, module, baseDir, settings)
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    val sbtVersion = maxOf(Sbt.LatestVersion(), MIN_RECOMMENDED_SBT_VERSION)
    val templateVariables = mapOf(
      PROJECT_NAME to sanitizeName(project.name),
      "SBT_VERSION" to sbtVersion.toString()
    )

    GeneratorUtils.createFileFromTemplate(baseDir, BUILD_SBT, BUILD_SBT, templateVariables)
    GeneratorUtils.createFileFromTemplate(
      baseDir,
      "${Sbt.ProjectDirectory()}/${Sbt.PropertiesFile()}",
      Sbt.PropertiesFile(),
      templateVariables
    )
  }

  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    setupSbtSettings(project)
  }

  private fun setupSbtSettings(project: Project) {
    val location = project.basePath!!
    val systemSettings = ExternalSystemApiUtil.getSettings(project, SbtProjectSystem.Id())

    val projectSettings = SbtProjectSettings()
    projectSettings.isUseAutoImport = true
    projectSettings.externalProjectPath = location

    val projects = ContainerUtilRt.newHashSet<Any>(systemSettings.getLinkedProjectsSettings())
    projects.add(projectSettings)
    systemSettings.setLinkedProjectsSettings(projects)
    ExternalSystemUtil.ensureToolWindowInitialized(project, SbtProjectSystem.Id())
  }

  companion object {
    // Minimal version of sbt that supports java 13
    private val MIN_RECOMMENDED_SBT_VERSION: Version = Version("1.3.3")
  }
}
