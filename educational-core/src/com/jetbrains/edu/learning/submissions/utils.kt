package com.jetbrains.edu.learning.submissions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader.CLOSE_PLACEHOLDER_TAG
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader.OPEN_PLACEHOLDER_TAG
import com.jetbrains.edu.learning.stepik.api.SolutionFile

fun getSolutionFiles(project: Project, task: Task, logger: Logger): List<SolutionFile>? {
  val files = mutableListOf<SolutionFile>()
  val taskDir: VirtualFile? = task.getDir(project.courseDir)
  if (taskDir == null) {
    logger.error("Failed to find task directory ${task.name}")
    return null
  }
  for (taskFile in task.taskFiles.values) {
    val fileName = taskFile.name
    val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
    ApplicationManager.getApplication().runReadAction {
      val document = FileDocumentManager.getInstance().getDocument(virtualFile) ?: return@runReadAction
      val text = document.text
      var insertedTextLength = 0
      val builder = StringBuilder(text)
      for (placeholder in taskFile.answerPlaceholders) {
        builder.insert(placeholder.offset + insertedTextLength, OPEN_PLACEHOLDER_TAG)
        builder.insert(placeholder.offset + insertedTextLength + placeholder.length + OPEN_PLACEHOLDER_TAG.length, CLOSE_PLACEHOLDER_TAG)
        insertedTextLength += OPEN_PLACEHOLDER_TAG.length + CLOSE_PLACEHOLDER_TAG.length
      }
      files.add(SolutionFile(fileName, builder.toString(), taskFile.isVisible))
    }
  }
  return files.toList()
}