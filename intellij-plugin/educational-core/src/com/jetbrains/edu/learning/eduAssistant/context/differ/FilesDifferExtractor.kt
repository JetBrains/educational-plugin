package com.jetbrains.edu.learning.eduAssistant.context.differ

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.isFileUnchanged
import com.jetbrains.edu.learning.getTextFromTaskTextFile

fun getChangedContent(task: Task, taskFile: TaskFile, project: Project): String? {
  val language = task.course.languageById ?: return null
  val virtualFile = taskFile.getVirtualFile(project) ?: return null
  val currentFileContent = virtualFile.getTextFromTaskTextFile()
  val fileContentFromPreviousStep = taskFile.snapshotFileContent
  taskFile.snapshotFileContent = currentFileContent
  if (!isFileUnchanged(taskFile, project) && fileContentFromPreviousStep != null) {
    val beforePsiFile = PsiFileFactory.getInstance(project).createFileFromText("fileFromPreviousStep", language, fileContentFromPreviousStep)
    val afterPsiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: return null
    return FilesDiffer.findDifferentMethods(beforePsiFile, afterPsiFile, taskFile, language)
  } else {
    return currentFileContent
  }
}
