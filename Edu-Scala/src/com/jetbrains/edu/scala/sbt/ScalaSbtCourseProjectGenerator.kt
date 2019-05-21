package com.jetbrains.edu.scala.sbt

import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtilRt
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.EduNames.PROJECT_NAME
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createChildFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.evaluateExistingTemplate
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.sanitizeName
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.scala.sbt.ScalaSbtCourseBuilder.Companion.BUILD_SBT
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.settings.SbtProjectSettings

class ScalaSbtCourseProjectGenerator(builder: ScalaSbtCourseBuilder, course: Course) : CourseProjectGenerator<JdkProjectSettings>(builder, course) {
  override fun createCourseStructure(project: Project, baseDir: VirtualFile, settings: JdkProjectSettings) {
    GeneratorUtils.renameBaseModule(project)
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)
    super.createCourseStructure(project, baseDir, settings)
  }

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    val child = baseDir.findChild(BUILD_SBT)
    val templateVariables = mapOf(PROJECT_NAME to sanitizeName(project.name))

    if (child == null) {
      val configText = getInternalTemplateText(BUILD_SBT, templateVariables)
      createChildFile(baseDir, BUILD_SBT, configText)
    }
    else {
      evaluateExistingTemplate(child, templateVariables)
    }
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
}