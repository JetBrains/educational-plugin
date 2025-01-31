package com.jetbrains.edu.learning.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerListener
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.ide.SaveAndSyncHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.actions.RunTaskAction.Companion.RUN_CONFIGURATION_FILE_NAME
import java.util.concurrent.ConcurrentHashMap

class PreventRunConfigurationsRenameListener(private val project: Project) : RunManagerListener {

  private val runConfigurationFiles: ConcurrentHashMap<RunnerAndConfigurationSettings, String> = ConcurrentHashMap()

  override fun runConfigurationChanged(settings: RunnerAndConfigurationSettings) {
    if (!project.isEduProject()) return
    val settingsPath = settings.pathIfStoredInArbitraryFileInProject ?: return

    if (settingsPath.endsWith("/$RUN_CONFIGURATION_FILE_NAME")) {
      runConfigurationFiles[settings] = settingsPath
    }
  }

  override fun endUpdate() {
    if (!project.isEduProject()) return

    var modified = false
    for ((settings, settingsPath) in runConfigurationFiles) {
      if (settings.pathIfStoredInArbitraryFileInProject != settingsPath) {
        settings.storeInArbitraryFileInProject(settingsPath)

        RunManager.getInstance(project).addConfiguration(settings)
        LOG.info("Prevented rename of run configuration at `$settingsPath`")
        modified = true
      }
    }
    runConfigurationFiles.clear()

    if (modified) {
      SaveAndSyncHandler.getInstance().scheduleProjectSave(project)
    }
  }

  companion object {
    private val LOG: Logger = logger<PreventRunConfigurationsRenameListener>()
  }
}
