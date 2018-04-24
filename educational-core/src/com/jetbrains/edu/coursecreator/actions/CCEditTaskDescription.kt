package com.jetbrains.edu.coursecreator.actions

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.ext.addDefaultTaskDescription
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

class CCEditTaskDescription : DumbAwareAction(TEXT, TEXT, AllIcons.Modules.Edit) {
  companion object {
    private const val TEXT = "Edit task description"
  }

  override fun actionPerformed(e: AnActionEvent?) {
    val project = e?.project ?: return
    val task = EduUtils.getCurrentTask(project) ?: return
    val descriptionFile = findOrCreateDescriptionFile(project, task)
    FileEditorManager.getInstance(project).openFile(descriptionFile, true)
  }

  private fun findOrCreateDescriptionFile(project: Project, task: Task): VirtualFile {
    val descriptionFile = task.getDescriptionFile(project)
    if (descriptionFile != null) return descriptionFile

    val taskDir = task.getTaskDir(project) ?: error("Task dir for task ${task.name} not found")
    if (task.descriptionText == null) {
      task.addDefaultTaskDescription()
    }
    return GeneratorUtils.createDescriptionFile(taskDir, task) ?: error("Failed to create description file in $taskDir")
  }

  override fun update(e: AnActionEvent?) {
    val project = e?.project ?: return
    if (!CCUtils.isCourseCreator(project)) {
      e.presentation.isEnabledAndVisible = false
      return
    }
    e.presentation.isVisible = true
    e.presentation.isEnabled = EduUtils.getCurrentTask(project) != null
  }
}
