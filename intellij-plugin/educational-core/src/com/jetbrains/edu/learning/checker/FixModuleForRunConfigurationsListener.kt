package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener

/**
 * Sets up correct module for custom task run configuration if needed.
 *
 * Run configuration can use module to run the corresponding command properly.
 * But serialized form of run configuration keeps only module name and
 * if you create course project, your modules may have different names.
 * As a result, run configuration won't be properly set up by the platform.
 *
 * The work itself is done asynchronously by [FixModuleForRunConfigurationsService]
 */
class FixModuleForRunConfigurationsListener(private val project: Project) : RunManagerListener, ModuleRootListener {

  override fun runConfigurationAdded(settings: RunnerAndConfigurationSettings) {
    FixModuleForRunConfigurationsService.getInstance(project).fixConfigurationModules { listOf(settings) }
  }

  override fun rootsChanged(event: ModuleRootEvent) {
    FixModuleForRunConfigurationsService.getInstance(project).fixConfigurationModules { project.serviceAsync<RunManager>().allSettings }
  }
}
