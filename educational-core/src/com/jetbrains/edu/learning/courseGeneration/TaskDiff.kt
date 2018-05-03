package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
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

fun addFile(path: String, text: String): TaskDiff = FileAdded(path, text)
fun removeFile(path: String, text: String): TaskDiff = FileRemoved(path, text)
fun changeFile(path: String, prevText: String, nextText: String): TaskDiff =
  ItemChanged(removeFile(path, prevText), addFile(path, nextText))

fun addTaskFile(path: String, taskFile: TaskFile): TaskDiff = TaskFileAdded(path, taskFile)
fun removeTaskFile(path: String, taskFile: TaskFile): TaskDiff = TaskFileRemoved(path, taskFile)
fun changeTaskFile(path: String, prevTaskFile: TaskFile, nextTaskFile: TaskFile): TaskDiff =
  ItemChanged(removeTaskFile(path, prevTaskFile), addTaskFile(path, nextTaskFile))

private open class ReversedTaskDiff(private val diff: TaskDiff) : TaskDiff {
  override fun apply(project: Project, baseDir: VirtualFile) = diff.revert(project, baseDir)
  override fun revert(project: Project, baseDir: VirtualFile) = diff.apply(project, baseDir)
}

private class ItemChanged(private val remove: TaskDiff, private val add: TaskDiff) : TaskDiff {
  override fun apply(project: Project, baseDir: VirtualFile) {
    remove.apply(project, baseDir)
    add.apply(project, baseDir)
  }

  override fun revert(project: Project, baseDir: VirtualFile) {
    add.revert(project, baseDir)
    remove.revert(project, baseDir)
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
    runWriteAction {
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

private class FileRemoved(path: String, text: String) : ReversedTaskDiff(FileAdded(path, text))

private class TaskFileAdded(
  path: String,
  private val taskFile: TaskFile
) : TaskDiff {

  private val add: FileAdded = FileAdded(path, taskFile.text)

  override fun apply(project: Project, baseDir: VirtualFile) {
    add.apply(project, baseDir)
    restoreAnswers(project)
  }

  override fun revert(project: Project, baseDir: VirtualFile) {
    add.revert(project, baseDir)
  }

  private fun restoreAnswers(project: Project) {
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

private class TaskFileRemoved(path: String, taskFile: TaskFile) : ReversedTaskDiff(TaskFileAdded(path, taskFile))
