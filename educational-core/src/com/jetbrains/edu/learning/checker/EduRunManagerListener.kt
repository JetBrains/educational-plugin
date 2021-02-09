package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.isTaskRunConfigurationFile

/**
 * Sets up correct module for custom task run configuration if needed.
 *
 * Run configuration can use module to run the corresponding command properly.
 * But serialized form of run configuration keeps only module name and
 * if you create course project, your modules may have different names.
 * As a result, run configuration won't be properly set up by the platform.
 */
class EduRunManagerListener(private val project: Project) : RunManagerListener {
  override fun runConfigurationAdded(settings: RunnerAndConfigurationSettings) {
    if (!EduUtils.isEduProject(project)) return
    val path = settings.pathIfStoredInArbitraryFileInProject ?: return
    val configuration = settings.configuration as? ModuleBasedConfiguration<*, *> ?: return
    if (configuration.configurationModule.module != null) return

    val file = LocalFileSystem.getInstance().findFileByPath(path) ?: return
    if (file.isTaskRunConfigurationFile(project)) {
      configuration.setModule(ModuleUtilCore.findModuleForFile(file, project))
    }
  }
}