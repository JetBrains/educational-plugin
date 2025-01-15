package com.jetbrains.edu.learning.actions

import com.intellij.configurationStore.runInAutoSaveDisabledMode
import com.intellij.configurationStore.saveSettings
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.jetbrains.edu.learning.EduNames.RUN_CONFIGURATION_DIR
import com.jetbrains.edu.learning.actions.RunTaskAction.Companion.RUN_CONFIGURATION_FILE_NAME
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.selectedTaskFile

class AssignRunConfigurationToTask : AnAction(), DumbAware {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = false

    val project = e.project ?: return
    val course = project.course ?: return
    if (course.isStudy) return
    if (project.selectedTaskFile == null) return
    if (RunManager.getInstance(project).selectedConfiguration == null) return

    e.presentation.isEnabled = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val taskFile = project.selectedTaskFile ?: return
    val task = taskFile.task
    val selectedConfiguration = RunManager.getInstance(project).selectedConfiguration ?: return

    val taskDir = project.courseDir.findFileByRelativePath(task.pathInCourse) ?: return
    
    selectedConfiguration.name = "Run task: ${task.name} (${task.parent.name})"

    runWriteAction {
      selectedConfiguration.storeInArbitraryFileInProject("${taskDir.path}/$RUN_CONFIGURATION_DIR/$RUN_CONFIGURATION_FILE_NAME")
    }

    saveRunConfigurations(project, selectedConfiguration)

    EduNotificationManager.showInfoNotification(
      project = project,
      title = EduCoreBundle.message("actions.run.task.configuration.assigned.title"),
      content = EduCoreBundle.message("actions.run.task.configuration.assigned.message", task.name, selectedConfiguration.name)
    )
  }

  private fun saveRunConfigurations(project: Project, selectedConfiguration: RunnerAndConfigurationSettings) {
    RunManager.getInstance(project).addConfiguration(selectedConfiguration)
    runInAutoSaveDisabledMode {
      runWithModalProgressBlocking(project, "") {
        saveSettings(project)
      }
    }
  }

  companion object {
    const val ACTION_ID = "Educational.AssignRunConfigurationToTask"
  }
}