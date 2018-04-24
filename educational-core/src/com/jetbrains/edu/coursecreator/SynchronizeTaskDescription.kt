package com.jetbrains.edu.coursecreator

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils

class SynchronizeTaskDescription(val project: Project): DocumentListener {
  override fun documentChanged(event: DocumentEvent?) {
    val eventDocument = event?.document ?: return
    val editedFile = FileDocumentManager.getInstance().getFile(eventDocument) ?: return
    val task = EduUtils.getTaskForFile(project, editedFile) ?: return
    task.descriptionText = eventDocument.text
    EduUtils.updateToolWindows(project)
  }
}
