package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction

class TestPycAction : DumbAwareAction("test pyc file") {
  override fun actionPerformed(p0: AnActionEvent) {
    val selectedFiles = CommonDataKeys.VIRTUAL_FILE_ARRAY.getData(p0.dataContext)
    val testFile = selectedFiles?.first() ?: throw IllegalStateException()
    val project = p0.project ?: throw IllegalStateException()
    FileEditorManager.getInstance(project).openFile(testFile, false)
  }
}