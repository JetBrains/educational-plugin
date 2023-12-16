package com.jetbrains.edu.learning.checker.details

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.messages.EduCoreBundle

class CheckDetailsToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.stripeTitle = EduCoreBundle.message("check.details.title")
    toolWindow.setIcon(EducationalCoreIcons.CheckDetailsIcon)
    toolWindow.setToHideOnEmptyContent(true)
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object: FileEditorManagerListener {
      override fun selectionChanged(event: FileEditorManagerEvent) {
        toolWindow.setAvailable(false, null)
      }

      override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
        toolWindow.setAvailable(false, null)
      }
    })
  }

  override fun shouldBeAvailable(project: Project) = false

  companion object {
    const val ID = "Educational.CheckDetails"
  }
}
