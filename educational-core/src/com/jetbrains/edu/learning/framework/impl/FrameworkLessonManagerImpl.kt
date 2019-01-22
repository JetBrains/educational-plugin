package com.jetbrains.edu.learning.framework.impl

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import java.io.IOException

class FrameworkLessonManagerImpl(private val project: Project) : FrameworkLessonManager {

  private val storage: FrameworkStorage = FrameworkStorage(constructStoragePath(project))

  override fun prepareNextTask(lesson: FrameworkLesson, taskDir: VirtualFile) {
    applyTargetTaskChanges(lesson, 1, taskDir)
  }

  override fun preparePrevTask(lesson: FrameworkLesson, taskDir: VirtualFile) {
    applyTargetTaskChanges(lesson, -1, taskDir)
  }

  private fun applyTargetTaskChanges(lesson: FrameworkLesson, taskIndexDelta: Int, taskDir: VirtualFile) {
    check(EduUtils.isStudentProject(project)) {
      "`applyTargetTaskChanges` should be called only if course in study mode"
    }
    val currentTaskIndex = lesson.currentTaskIndex
    val targetTaskIndex = currentTaskIndex + taskIndexDelta

    val currentTask = lesson.taskList[currentTaskIndex]
    val targetTask = lesson.taskList[targetTaskIndex]

    lesson.currentTaskIndex = targetTaskIndex

    val currentRecord = currentTask.record
    val targetRecord = targetTask.record

    val initialCurrentFiles = currentTask.allFiles
    val (newCurrentRecord, currentUserChanges) = try {
      updateUserChanges(currentRecord, initialCurrentFiles, taskDir)
    } catch (e: IOException) {
      LOG.error("Failed to save user changes for task `${currentTask.name}`", e)
      UpdatedUserChanges(currentRecord, emptyMap())
    }

    currentTask.record = newCurrentRecord

    val nextUserChanges = try {
      storage.getUserChanges(targetRecord).changes
    } catch (e: IOException) {
      LOG.error("Failed to get user changes for task `${currentTask.name}`", e)
      emptyMap<String, String>()
    }

    val changes = calculateChanges(initialCurrentFiles + currentUserChanges, targetTask.allFiles + nextUserChanges)
    for (change in changes) {
      change.apply(project, taskDir)
    }
  }

  @Synchronized
  private fun updateUserChanges(record: Int, initialFiles: Map<String, String>, taskDir: VirtualFile): UpdatedUserChanges {
    val documentManager = FileDocumentManager.getInstance()

    val changes = HashMap<String, String>()
    for ((path, originalText) in initialFiles) {
      val file = taskDir.findFileByRelativePath(path) ?: continue
      val text = runReadAction { documentManager.getDocument(file)?.text } ?: continue
      if (text != originalText) {
        changes[path] = text
      }
    }

    return try {
      val newRecord = storage.updateUserChanges(record, UserChanges(changes))
      storage.force()
      UpdatedUserChanges(newRecord, changes)
    } catch (e: IOException) {
      LOG.error("Failed to update user changes", e)
      UpdatedUserChanges(record, emptyMap())
    }
  }

  private fun calculateChanges(
    currentState: Map<String, String>,
    nextState: Map<String, String>
  ): List<Change> {
    val changes = mutableListOf<Change>()
    val current = HashMap(currentState)
    for ((path, nextText) in nextState) {
      val currentText = current.remove(path)
      changes += if (currentText == null) {
        Change.AddFile(path, nextText)
      } else {
        Change.ChangeFile(path, nextText)
      }
    }

    current.mapTo(changes) { Change.RemoveFile(it.key) }
    return changes
  }

  private val Task.allFiles: Map<String, String> get() = taskFiles.mapValues { it.value.text }

  companion object {
    private val LOG: Logger = Logger.getInstance(FrameworkLessonManagerImpl::class.java)

    @VisibleForTesting
    fun constructStoragePath(project: Project): String =
      FileUtil.join(project.basePath!!, Project.DIRECTORY_STORE_FOLDER, "frameworkLessonHistory", "storage")
  }
}

private data class UpdatedUserChanges(
  val record: Int,
  val changes: Map<String, String>
)

private sealed class Change(protected val path: String) {

  abstract fun apply(project: Project, taskDir: VirtualFile)

  class AddFile(path: String, private val text: String) : Change(path) {
    override fun apply(project: Project, taskDir: VirtualFile) {
      try {
        GeneratorUtils.createChildFile(taskDir, path, text)
      } catch (e: IOException) {
        LOG.error("Failed to create file `${taskDir.path}/$path`", e)
      }
    }

  }
  class RemoveFile(path: String) : Change(path) {
    override fun apply(project: Project, taskDir: VirtualFile) {
      runUndoTransparentWriteAction {
        try {
          taskDir.findFileByRelativePath(path)?.delete(RemoveFile::class.java)
        } catch (e: IOException) {
          LOG.error("Failed to delete file `${taskDir.path}/$path`", e)
        }
      }
    }

  }
  class ChangeFile(path: String, private val text: String) : Change(path) {
    override fun apply(project: Project, taskDir: VirtualFile) {
      val file = taskDir.findFileByRelativePath(path)
      if (file == null) {
        LOG.warn("Can't find file `$path` in `$taskDir`")
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

  companion object {
    private val LOG: Logger = Logger.getInstance(Change::class.java)
  }
}
