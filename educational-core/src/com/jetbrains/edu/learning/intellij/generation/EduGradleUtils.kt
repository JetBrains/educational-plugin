package com.jetbrains.edu.learning.intellij.generation

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
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
    @Throws(IOException::class)
    fun createProjectGradleFiles(projectPath: String, projectName: String, buildGradleTemplateName: String) {
        val projectDir = VfsUtil.findFileByIoFile(File(FileUtil.toSystemDependentName(projectPath)), true) ?: return

        if (projectDir.findChild(GradleConstants.DEFAULT_SCRIPT_NAME) == null) {
            val buildTemplate = FileTemplateManager.getDefaultInstance().getInternalTemplate(buildGradleTemplateName)
            createChildFile(projectDir, GradleConstants.DEFAULT_SCRIPT_NAME, buildTemplate.text.replace("\$GRADLE_VERSION\$", gradleVersion()))
        }

        val settingsTemplate = FileTemplateManager.getDefaultInstance().getInternalTemplate(GradleConstants.SETTINGS_FILE_NAME)
        createChildFile(projectDir, GradleConstants.SETTINGS_FILE_NAME, settingsTemplate.text.replace("\$PROJECT_NAME\$", projectName))
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
    }

    @JvmStatic
    fun importGradleProject(project: Project, projectBasePath: String) {
        ExternalSystemUtil.refreshProjects(
          ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
            .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC))
    }

    private fun gradleVersion(): String = maxOf(GradleVersion.current(), GradleVersion.version(DEFAULT_GRADLE_VERSION)).version
}
