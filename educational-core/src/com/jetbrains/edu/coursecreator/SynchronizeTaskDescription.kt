package com.jetbrains.edu.coursecreator

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView

class SynchronizeTaskDescription(val project: Project): DocumentListener {
  override fun documentChanged(event: DocumentEvent) {
    val eventDocument = event.document
    val editedFile = FileDocumentManager.getInstance().getFile(eventDocument) ?: return
    if (editedFile is LightVirtualFile || !EduUtils.isTaskDescriptionFile(editedFile.name)) {
      return
    }
    val task = EduUtils.getTaskForFile(project, editedFile) ?: return
    task.descriptionText = eventDocument.text
    TaskDescriptionView.getInstance(project).updateTaskDescription(task)
  }
}
