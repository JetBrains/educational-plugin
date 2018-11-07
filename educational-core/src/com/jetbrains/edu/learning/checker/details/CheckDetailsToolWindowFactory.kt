package com.jetbrains.edu.learning.checker.details

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class CheckDetailsToolWindowFactory : ToolWindowFactory {
  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    toolWindow.stripeTitle = "Check Details"
    toolWindow.isToHideOnEmptyContent = true
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

  override fun isDoNotActivateOnStart() = true

  companion object {
    const val ID = "Educational.CheckDetails"
  }
}

class CheckDetailsToolWindowCondition : Condition<Any> {
  override fun value(t: Any?) = true
}