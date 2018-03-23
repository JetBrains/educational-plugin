@file:JvmName("TaskFileExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.TaskFile


fun TaskFile.getDocument(project: Project): Document? {
  val virtualFile = getVirtualFile(project) ?: return null
  return FileDocumentManager.getInstance().getDocument(virtualFile)
}

fun TaskFile.getVirtualFile(project: Project): VirtualFile? {
  val taskDir = task.getTaskDir(project) ?: return null
  return EduUtils.findTaskFileInDir(this, taskDir)
}