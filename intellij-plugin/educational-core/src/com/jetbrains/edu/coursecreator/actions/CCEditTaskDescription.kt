package com.jetbrains.edu.coursecreator.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.addDefaultTaskDescription
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class CCEditTaskDescription : DumbAwareAction(
  EduCoreBundle.lazyMessage("action.edit.task.description.text"),
  EduCoreBundle.lazyMessage("action.edit.task.description.description"),
  AllIcons.Actions.Edit
) {
  companion object {
    @NonNls
    const val ACTION_ID = "Educational.CCEditTaskDescription"
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    val descriptionFile = findOrCreateDescriptionFile(project, task)
    FileEditorManager.getInstance(project).openFile(descriptionFile, true)
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  private fun findOrCreateDescriptionFile(project: Project, task: Task): VirtualFile {
    val descriptionFile = task.getDescriptionFile(project)
    if (descriptionFile != null) return descriptionFile

    val taskDir = task.getDir(project.courseDir) ?: error("Task dir for task ${task.name} not found")
    if (task.descriptionText.isEmpty()) {
      task.addDefaultTaskDescription()
    }
    return GeneratorUtils.createDescriptionFile(project, taskDir, task) ?: error("Failed to create description file in $taskDir")
  }

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    if (!CCUtils.isCourseCreator(project)) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isVisible = true
    e.presentation.isEnabled = project.getCurrentTask() != null
  }
}
