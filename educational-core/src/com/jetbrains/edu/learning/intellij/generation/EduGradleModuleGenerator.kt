package com.jetbrains.edu.learning.intellij.generation

import com.intellij.ide.fileTemplates.FileTemplateManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.createChildFile
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.io.IOException

object EduGradleModuleGenerator {

    @JvmStatic
    @Throws(IOException::class)
    fun createProjectGradleFiles(projectPath: String, projectName: String, buildGradleTemplateName: String) {
        val projectDir = VfsUtil.findFileByIoFile(File(FileUtil.toSystemDependentName(projectPath)), true) ?: return

        val buildTemplate = FileTemplateManager.getDefaultInstance().getInternalTemplate(buildGradleTemplateName)
        createChildFile(projectDir, GradleConstants.DEFAULT_SCRIPT_NAME, buildTemplate.text)

        val settingsTemplate = FileTemplateManager.getDefaultInstance().getInternalTemplate(GradleConstants.SETTINGS_FILE_NAME)
        createChildFile(projectDir, GradleConstants.SETTINGS_FILE_NAME, settingsTemplate.text.replace("\$PROJECT_NAME\$", projectName))
    }
}
