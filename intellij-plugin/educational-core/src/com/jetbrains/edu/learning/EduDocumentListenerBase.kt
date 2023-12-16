package com.jetbrains.edu.learning

import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.courseFormat.Course

abstract class EduDocumentListenerBase(protected val holder: CourseInfoHolder<out Course?>) : DocumentListener {

  constructor(project: Project) : this(project.toCourseInfoHolder())

  protected val fileDocumentManager: FileDocumentManager = FileDocumentManager.getInstance()

  protected fun DocumentEvent.isInProjectContent(): Boolean {
    val file = fileDocumentManager.getFile(document) ?: return false
    return VfsUtil.isAncestor(holder.courseDir, file, true)
  }
}
