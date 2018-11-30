package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import java.io.IOException

/**
 * Represent difference between two neighbor tasks in framework lesson.
 *
 * Note, initial state and state after sequential invocation of `apply` and `revert` can differ
 */
interface TaskDiff {
  fun apply(project: Project, baseDir: VirtualFile)
  fun revert(project: Project, baseDir: VirtualFile)
}

fun addTaskFile(path: String, taskFile: TaskFile): TaskDiff = TaskFileAdded(path, taskFile)
fun removeTaskFile(path: String, taskFile: TaskFile): TaskDiff = TaskFileRemoved(path, taskFile)
fun changeTaskFile(path: String, prevTaskFile: TaskFile, nextTaskFile: TaskFile): TaskDiff = TaskFileChanged(path, prevTaskFile, nextTaskFile)

private abstract class TaskFileDiff(
  private val baseDiff: TaskDiff,
  private val prevTaskFile: TaskFile?,
  private val nextTaskFile: TaskFile?
): TaskDiff {
  override fun apply(project: Project, baseDir: VirtualFile) {
    baseDiff.apply(project, baseDir)
    restoreAnswers(project, nextTaskFile)
  }

  override fun revert(project: Project, baseDir: VirtualFile) {
    baseDiff.revert(project, baseDir)
    restoreAnswers(project, prevTaskFile)
  }

  companion object {
    private fun restoreAnswers(project: Project, taskFile: TaskFile?) {
      if (taskFile == null) return
      // Sort placeholders to avoid offset recalculation after each replacement
      val sortedPlaceholders = taskFile.answerPlaceholders.sortedByDescending { it.initialState.offset }
      for (placeholder in sortedPlaceholders) {
        val studentAnswer = placeholder.studentAnswer
        if (studentAnswer != null) {
          val startOffset = placeholder.initialState.offset
          val endOffset = startOffset + placeholder.initialState.length
          runUndoTransparentWriteAction {
            taskFile.getDocument(project)?.replaceString(startOffset, endOffset, studentAnswer)
          }
        }
      }
    }
  }
}

private class FileAdded(
  private val path: String,
  private val text: String
) : TaskDiff {
  override fun apply(project: Project, baseDir: VirtualFile) {
    try {
      GeneratorUtils.createChildFile(baseDir, path, text)
    } catch (e: IOException) {
      LOG.error("Failed to create file `${baseDir.path}/$path`", e)
    }
  }

  override fun revert(project: Project, baseDir: VirtualFile) {
    runUndoTransparentWriteAction {
      try {
        baseDir.findFileByRelativePath(path)?.delete(FileAdded::class.java)
      } catch (e: IOException) {
        LOG.error("Failed to delete file `${baseDir.path}/$path`", e)
      }
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(FileAdded::class.java)
  }
}

private class FileRemoved(path: String, text: String) : TaskDiff {

  private val add: FileAdded = FileAdded(path, text)

  override fun apply(project: Project, baseDir: VirtualFile) = add.revert(project, baseDir)
  override fun revert(project: Project, baseDir: VirtualFile) = add.apply(project, baseDir)
}

private class FileChanged(
  private val path: String,
  private val prevText: String,
  private val nextText: String
) : TaskDiff {
  override fun apply(project: Project, baseDir: VirtualFile) {
    setText(baseDir, path, nextText)
  }

  override fun revert(project: Project, baseDir: VirtualFile) {
    setText(baseDir, path, prevText)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(FileChanged::class.java)

    private fun setText(baseDir: VirtualFile, path: String, text: String) {
      val file = baseDir.findFileByRelativePath(path)
      if (file == null) {
        LOG.warn("Can't find file `$path` in `$baseDir`")
        return
      }

      val document = runReadAction { FileDocumentManager.getInstance().getDocument(file) }
      if (document == null) {
        LOG.warn("Can't get document for `$file`")
      } else {
        runUndoTransparentWriteAction { document.setText(text) }
      }
    }
  }
}

private class TaskFileAdded(path: String, nextTaskFile: TaskFile) :
  TaskFileDiff(FileAdded(path, nextTaskFile.getText()), null, nextTaskFile)

private class TaskFileRemoved(path: String, prevTaskFile: TaskFile) :
  TaskFileDiff(FileRemoved(path, prevTaskFile.getText()), prevTaskFile, null)

private class TaskFileChanged(path: String, prevTaskFile: TaskFile, nextTaskFile: TaskFile) :
  TaskFileDiff(FileChanged(path, prevTaskFile.getText(), nextTaskFile.getText()), prevTaskFile, nextTaskFile)
