package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ModuleBasedConfiguration
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.courseFormat.ext.findSourceDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Sets up correct module for task run configurations on behalf of [FixModuleForRunConfigurationsListener].
 *
 * It's a separate service to be able to replace it in tests and wait until the work is done
 */
@Service(Service.Level.PROJECT)
class FixModuleForRunConfigurationsService(
  private val project: Project,
  private val scope: CoroutineScope
) {

  private fun learnerMode(): Boolean = project.isEduProject() && project.course?.isStudy == true

  /**
   * Asynchronously sets up correct module for task run configurations from [getSettings].
   * Returns the job doing the work, mostly for tests to wait for it
   */
  fun fixConfigurationModules(getSettings: suspend () -> List<RunnerAndConfigurationSettings>): Job {
    // `learnerMode` may initialize `StudyTaskManager` because it's called early enough to be the first call of `StudyTaskManager.getInstance`.
    // And since listener methods may be called in write action (e.g. `rootsChanged`),
    // it may lead to deadlock because of using internal lock and read action in `StudyTaskManager` initialization.
    // So this code is invoked outside write action to avoid it.
    // It's done in background thread since course loading and module lookup are slow operations and shouldn't be done on EDT
    return scope.launch(Dispatchers.Default) {
      if (!learnerMode()) return@launch
      val settings = getSettings()
      val modules = readAction {
        settings.mapNotNull { findModuleForConfiguration(it) }
      }
      if (modules.isEmpty()) return@launch
      withContext(Dispatchers.EDT) {
        for ((configuration, module) in modules) {
          configuration.setModule(module)
        }
      }
    }
  }

  /**
   * Returns module-based configuration from [settings] with the module it should use,
   * or `null` if [settings] is not a task run configuration
   */
  private fun findModuleForConfiguration(settings: RunnerAndConfigurationSettings): TaskConfigurationModule? {
    val path = settings.pathIfStoredInArbitraryFileInProject ?: return null
    val configuration = settings.configuration as? ModuleBasedConfiguration<*, *> ?: return null

    val file = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
    if (!file.isTaskRunConfigurationFile(project)) return null
    // It's essential to search module for src dir because in some cases (Gradle-based projects)
    // modules of task directory and source directory may differ
    return when (val result = findSrcDir(file)) {
      is Ok -> TaskConfigurationModule(configuration, ModuleUtilCore.findModuleForFile(result.value, project))
      is Err -> {
        LOG.error(result.error)
        null
      }
    }
  }

  private fun findSrcDir(file: VirtualFile): Result<VirtualFile, String> {
    val taskDir = file.getTaskDir(project) ?: return Err("Failed to find task dir for $file")
    val task = taskDir.getContainingTask(project) ?: return Err("Failed to find task for $taskDir")
    val srcDir = task.findSourceDir(taskDir) ?: return Err("Failed to find source dir for ${task.name} task")
    return Ok(srcDir)
  }

  private data class TaskConfigurationModule(val configuration: ModuleBasedConfiguration<*, *>, val module: Module?)

  companion object {
    private val LOG: Logger = logger<FixModuleForRunConfigurationsService>()

    fun getInstance(project: Project): FixModuleForRunConfigurationsService = project.service()
  }
}
