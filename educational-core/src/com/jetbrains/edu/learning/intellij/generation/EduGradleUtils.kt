package com.jetbrains.edu.learning.intellij.generation

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.service.project.ExternalProjectRefreshCallback
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
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
    fun getInternalTemplateText(templateName: String, configVariables: Map<String, String>)  =
      FileTemplateManager.getDefaultInstance().getInternalTemplate(templateName)?.getText(configVariables)


    @JvmStatic
    @Throws(IOException::class)
    fun createProjectGradleFiles(
      projectPath: String,
      configTemplates: Map<String, String>,
      configVariables: Map<String, String>
    ) {
        val projectDir = VfsUtil.findFileByIoFile(File(FileUtil.toSystemDependentName(projectPath)), true) ?: return
        for ((name, templateName) in configTemplates) {
            if (projectDir.findChild(name) == null) {
              val configText = getInternalTemplateText(templateName, configVariables) ?: continue
                createChildFile(projectDir, name, configText)
            }
        }
   }

    @JvmStatic
    fun setGradleSettings(project: Project, location: String) {
        val systemSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
        val existingProject = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).getLinkedProjectSettings(location)
        if (existingProject is GradleProjectSettings) {
            if (existingProject.distributionType == null) {
                existingProject.distributionType = DistributionType.WRAPPED
            }
            if (existingProject.externalProjectPath == null) {
                existingProject.externalProjectPath = location
            }
            return
        }

        val gradleProjectSettings = GradleProjectSettings()
        gradleProjectSettings.distributionType = DistributionType.WRAPPED
        gradleProjectSettings.isUseAutoImport = true
        gradleProjectSettings.externalProjectPath = location

        val projects = ContainerUtilRt.newHashSet<Any>(systemSettings.getLinkedProjectsSettings())
        projects.add(gradleProjectSettings)
        systemSettings.setLinkedProjectsSettings(projects)
        ExternalSystemUtil.ensureToolWindowInitialized(project, GradleConstants.SYSTEM_ID)
    }

    @JvmOverloads
    @JvmStatic
    fun importGradleProject(project: Project, projectBasePath: String, callback: ExternalProjectRefreshCallback? = null) {
      val builder = ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
        .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
        .dontReportRefreshErrors()
      if (callback == null) {
        builder.useDefaultCallback()
      } else {
        builder.callback(object : ExternalProjectRefreshCallback {
          override fun onSuccess(externalProject: DataNode<ProjectData>?) {
            // We have to import data manually because we use custom callback
            // but default callback code is private.
            // See `com.intellij.openapi.externalSystem.importing.ImportSpecBuilder#build`
            if (externalProject != null) {
              ServiceManager.getService(ProjectDataManager::class.java).importData(externalProject, project, false)
            }
            callback.onSuccess(externalProject)
          }

          override fun onFailure(errorMessage: String, errorDetails: String?) {
            callback.onFailure(errorMessage, errorDetails)
          }
        })
      }
      // Build toolwindow will be opened if `ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT` is true while sync
      project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, null)
      ExternalSystemUtil.refreshProject(projectBasePath, builder.build())
    }

    @JvmStatic
    fun gradleVersion(): String = maxOf(GradleVersion.current(), GradleVersion.version(DEFAULT_GRADLE_VERSION)).version

    private val INVALID_SYMBOLS = "[ /\\\\:<>\"?*|]".toRegex()

    /**
     * Replaces ' ', '/', '\', ':', '<', '>', '"', '?', '*', '|' symbols with '_' as they are invalid in gradle module names
     */
    fun sanitizeName(name: String): String = name.replace(INVALID_SYMBOLS, "_")
}
