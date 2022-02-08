@file:JvmName("SubmissionUtils")

package com.jetbrains.edu.learning.submissions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader.CLOSE_PLACEHOLDER_TAG
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader.OPEN_PLACEHOLDER_TAG

private const val MAX_FILE_SIZE: Int = 5 * 1024 * 1024 // 5 Mb
private val LOG: Logger = Logger.getInstance(EduUtils::class.java.name)

fun getSolutionFiles(project: Project, task: Task): List<SolutionFile> {
  val files = ArrayList<SolutionFile>()
  val taskDir = task.getDir(project.courseDir) ?: error("Failed to find task directory ${task.name}")

  for (taskFile in task.taskFiles.values) {
    val virtualFile = EduUtils.findTaskFileInDir(taskFile, taskDir) ?: continue
    if (virtualFile.length > MAX_FILE_SIZE) {
      LOG.warn("File ${virtualFile.path} is too big (${virtualFile.length} bytes), will be ignored for submitting to the server")
      continue
    }

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
      files.add(SolutionFile(taskFile.name, builder.toString(), taskFile.isVisible))
    }
  }

  if (files.isEmpty()) {
    error("No files were collected to post solution")
  }

  return files
}