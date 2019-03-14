package com.jetbrains.edu.learning.gradle.generation

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.ide.fileTemplates.FileTemplateUtil
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtilRt
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createChildFile
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.io.IOException

object EduGradleUtils {
  private const val DEFAULT_GRADLE_VERSION = "4.5"

  @JvmStatic
  fun isConfiguredWithGradle(project: Project): Boolean {
    return File(project.basePath, GradleConstants.DEFAULT_SCRIPT_NAME).exists()
  }

  @JvmStatic
  fun getInternalTemplateText(templateName: String, templateVariables: Map<String, Any>) =
    FileTemplateManager.getDefaultInstance().getInternalTemplate(templateName).getText(templateVariables)


  @JvmStatic
  @Throws(IOException::class)
  fun createProjectGradleFiles(
    projectDir: VirtualFile,
    templates: Map<String, String>,
    templateVariables: Map<String, Any>
  ) {
    for ((name, templateName) in templates) {
      val child = projectDir.findChild(name)
      if (child == null) {
        val configText = getInternalTemplateText(templateName, templateVariables)
        createChildFile(projectDir, name, configText)
      }
      else {
        evaluateExistingTemplate(child, templateVariables)
      }
    }
  }

  @Throws(IOException::class)
  private fun evaluateExistingTemplate(child: VirtualFile, templateVariables: Map<String, Any>) {
    val rawContent = VfsUtil.loadText(child)
    val content = FileTemplateUtil.mergeTemplate(templateVariables, rawContent, false)
    // BACKCOMPAT: 2018.3
    @Suppress("DEPRECATION")
    invokeAndWaitIfNeed { runWriteAction { VfsUtil.saveText(child, content) } }
  }

  @JvmOverloads
  @JvmStatic
  fun setGradleSettings(project: Project, location: String, distributionType: DistributionType = DistributionType.WRAPPED) {
    val systemSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
    val existingProject = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).getLinkedProjectSettings(location)
    if (existingProject is GradleProjectSettings) {
      if (existingProject.distributionType == null) {
        existingProject.distributionType = distributionType
      }
      if (existingProject.externalProjectPath == null) {
        existingProject.externalProjectPath = location
      }
      return
    }

    val gradleProjectSettings = GradleProjectSettings()
    gradleProjectSettings.distributionType = distributionType
    gradleProjectSettings.isUseAutoImport = true
    gradleProjectSettings.externalProjectPath = location

    val projects = ContainerUtilRt.newHashSet<Any>(systemSettings.getLinkedProjectsSettings())
    projects.add(gradleProjectSettings)
    systemSettings.setLinkedProjectsSettings(projects)
    ExternalSystemUtil.ensureToolWindowInitialized(project, GradleConstants.SYSTEM_ID)
  }

  @JvmStatic
  fun gradleVersion(): String = maxOf(GradleVersion.current(), GradleVersion.version(DEFAULT_GRADLE_VERSION)).version

  private val INVALID_SYMBOLS = "[ /\\\\:<>\"?*|]".toRegex()

  /**
   * Replaces ' ', '/', '\', ':', '<', '>', '"', '?', '*', '|' symbols with '_' as they are invalid in gradle module names
   */
  fun sanitizeName(name: String): String = name.replace(INVALID_SYMBOLS, "_")
}
