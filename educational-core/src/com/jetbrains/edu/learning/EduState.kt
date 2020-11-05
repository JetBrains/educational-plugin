package com.jetbrains.edu.learning

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.editor.EduEditor


class EduState private constructor(eduEditor: EduEditor, val virtualFile: VirtualFile) {
  val editor: Editor = eduEditor.editor
  val taskFile: TaskFile = eduEditor.taskFile
  val task: Task = taskFile.task

  companion object {
    @JvmStatic
    fun getEduState(project: Project): EduState? {
      val eduEditor = EduUtils.getSelectedEduEditor(project) ?: return null
      val virtualFile = FileDocumentManager.getInstance().getFile(eduEditor.editor.document) ?: return null
      return EduState(eduEditor, virtualFile)
    }
  }
}