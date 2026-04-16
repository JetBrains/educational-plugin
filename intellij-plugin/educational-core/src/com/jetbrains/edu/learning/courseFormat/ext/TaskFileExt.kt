@file:JvmName("TaskFileExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.problems.WolfTheProblemSolver
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseFormat.TaskFile


fun TaskFile.getDocument(project: Project): Document? {
  val virtualFile = getVirtualFile(project) ?: return null
  return runReadAction { FileDocumentManager.getInstance().getDocument(virtualFile) }
}

fun TaskFile.getVirtualFile(project: Project): VirtualFile? {
  val taskDir = task.getDir(project.courseDir) ?: return null
  return this.findTaskFileInDir(taskDir)
}

fun TaskFile.findTaskFileInDir(taskDir: VirtualFile): VirtualFile? {
  return taskDir.findFileByRelativePath(name)
}

fun TaskFile.course() = task.lesson.course

fun TaskFile.getText(project: Project): String? = getDocument(project)?.text

val TaskFile.isTestFile: Boolean
  get() {
    val configurator = task.course.configurator ?: return false
    return configurator.isTestFile(task, name)
  }


fun TaskFile.revert(project: Project) {
  answerPlaceholders.forEach { answerPlaceholder ->
    answerPlaceholder.reset(true)
  }

  val virtualFile = getVirtualFile(project)
  if (virtualFile != null) {
    WolfTheProblemSolver.getInstance(project).clearProblems(virtualFile)
  }
  if (errorHighlightLevel == EduFileErrorHighlightLevel.ALL_PROBLEMS) {
    errorHighlightLevel = EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION
  }
}

fun TaskFile.getSolution(): String {
  val fullAnswer = StringBuilder(contents.textualRepresentation)
  val placeholders = answerPlaceholders.sortedBy { it.offset }.reversed()
  for (placeholder in placeholders) {
    placeholder.possibleAnswer
    fullAnswer.replace(
      placeholder.initialState.offset,
      placeholder.initialState.offset + placeholder.initialState.length,
      placeholder.possibleAnswer
    )
  }
  return fullAnswer.toString()
}

fun TaskFile.shouldBePropagated(): Boolean = isEditable && isVisible