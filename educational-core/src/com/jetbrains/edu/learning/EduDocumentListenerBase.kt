package com.jetbrains.edu.learning

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtil

abstract class EduDocumentListenerBase(protected val project: Project) : DocumentListener {

  protected val fileDocumentManager: FileDocumentManager = FileDocumentManager.getInstance()

  protected fun DocumentEvent.isInProjectContent(): Boolean {
    val file = fileDocumentManager.getFile(document) ?: return false
    val projectDir = project.guessProjectDir() ?: return false
    return VfsUtil.isAncestor(projectDir, file, true)
  }
}
