@file:JvmName("TaskFileExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils.loadText
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.editor.EduEditor
import com.jetbrains.edu.learning.editor.EduSplitEditor


fun TaskFile.getDocument(project: Project): Document? {
  val virtualFile = getVirtualFile(project) ?: return null
  return runReadAction { FileDocumentManager.getInstance().getDocument(virtualFile) }
}

fun TaskFile.getVirtualFile(project: Project): VirtualFile? {
  val taskDir = task.getTaskDir(project) ?: return null
  return EduUtils.findTaskFileInDir(this, taskDir)
}

fun TaskFile.course() = task?.lesson?.course

fun TaskFile.getTextFromDisk(project: Project): String? {
  val virtualFile = getVirtualFile(project) ?: return null
  return loadText(virtualFile)
}

fun TaskFile.getEduEditors(project: Project): List<EduEditor> {
  val file = getVirtualFile(project) ?: return emptyList()
  return FileEditorManager.getInstance(project)
    .allEditors
    .asSequence()
    .flatMap {
      if (it is EduSplitEditor) {
        sequenceOf(it.mainEditor, it.secondaryEditor)
      }
      else {
        sequenceOf(it)
      }
    }
    .filterIsInstance<EduEditor>()
    .filter { it.file == file }
    .toList()
}
