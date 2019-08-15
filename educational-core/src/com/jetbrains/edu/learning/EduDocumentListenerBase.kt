package com.jetbrains.edu.learning

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex

abstract class EduDocumentListenerBase(protected val project: Project) : DocumentListener {

  protected val fileDocumentManager: FileDocumentManager = FileDocumentManager.getInstance()
  private val projectFileIndex: ProjectFileIndex = ProjectFileIndex.getInstance(project)

  protected fun DocumentEvent.isInProjectContent(): Boolean {
    val file = fileDocumentManager.getFile(document) ?: return false
    return projectFileIndex.isInContent(file)
  }
}
