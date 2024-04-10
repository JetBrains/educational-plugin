package com.jetbrains.edu.coursecreator

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.learning.EduDocumentListenerBase
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.EduUtilsKt.isEduProject
import com.jetbrains.edu.learning.getContainingTask
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowFactory.Companion.STUDY_TOOL_WINDOW
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView

class SynchronizeTaskDescription(private val project: Project) : EduDocumentListenerBase(project) {

  override fun documentChanged(event: DocumentEvent) {
    if (!event.isInProjectContent()) return
    if (!project.isEduProject()) return
    val eventDocument = event.document
    val editedFile = fileDocumentManager.getFile(eventDocument) ?: return
    if (editedFile is LightVirtualFile || !EduUtilsKt.isTaskDescriptionFile(editedFile.name)) {
      return
    }
    val task = editedFile.getContainingTask(project) ?: return
    task.descriptionText = eventDocument.text
    if (ToolWindowManager.getInstance(project).getToolWindow(STUDY_TOOL_WINDOW) == null) return

    TaskToolWindowView.getInstance(project).updateTaskDescriptionTab(task)
  }
}
