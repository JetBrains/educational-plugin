package com.jetbrains.edu.coursecreator

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.learning.EduDocumentListenerBase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView

class SynchronizeTaskDescription(project: Project): EduDocumentListenerBase(project) {
  override fun documentChanged(event: DocumentEvent) {
    if (!event.isInProjectContent()) return
    val eventDocument = event.document
    val editedFile = fileDocumentManager.getFile(eventDocument) ?: return
    if (editedFile is LightVirtualFile || !EduUtils.isTaskDescriptionFile(editedFile.name)) {
      return
    }
    val task = EduUtils.getTaskForFile(project, editedFile) ?: return
    task.descriptionText = eventDocument.text
    TaskDescriptionView.getInstance(project).updateTaskDescription(task)
  }
}
