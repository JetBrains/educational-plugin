@file:JvmName("TaskFileExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.TaskFile


fun TaskFile.getDocument(project: Project): Document? {
  val taskDir = task.getTaskDir(project) ?: return null
  val virtualFile = EduUtils.findTaskFileInDir(this, taskDir) ?: return null
  return FileDocumentManager.getInstance().getDocument(virtualFile)
}