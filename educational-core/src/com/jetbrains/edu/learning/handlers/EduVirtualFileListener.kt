package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.PlaceholderPainter
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.fileInfo
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

abstract class EduVirtualFileListener(protected val project: Project) : VirtualFileListener {

  override fun fileCreated(event: VirtualFileEvent) {
    if (event.file.isDirectory) return
    val fileInfo = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    fileInTaskCreated(fileInfo, event.file)
  }

  /**
   * Actual text of files is not loaded intentionally
   * because it is required only in some places where it is really needed:
   * course archive creation, loading to Stepik, etc.
   * Such actions load necessary text of files themselves.
   *
   * Also info about new file won't be added if the file is already in the task.
   * Generally, such checks are required because of tests.
   * In real life, project files are created before project opening and virtual file listener initialization,
   * so such situation shouldn't happen.
   * But in tests, course files usually are created by [EduTestCase.courseWithFiles] which triggers virtual file listener because
   * sometimes listener is initialized in `[TestCase.setUp] method and [EduTestCase.courseWithFiles] creates course files after it.
   * In such cases, these checks prevent replacing correct task file
   * with empty (without placeholders, hints, etc.) one.
   */
  protected open fun fileInTaskCreated(fileInfo: FileInfo.FileInTask, createFile: VirtualFile) {
    val (task, pathInTask) = fileInfo
    if (task.getTaskFile(pathInTask) == null) {
      val taskFile = task.addTaskFile(pathInTask)
      taskFileCreated(taskFile, createFile)
      YamlFormatSynchronizer.saveItem(fileInfo.task)
    }
  }

  override fun beforePropertyChange(event: VirtualFilePropertyEvent) {
    if (event.propertyName != VirtualFile.PROP_NAME) return
    val newName = event.newValue as? String ?: return
    val (task, oldPath) = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    val newPath = oldPath.replaceAfterLast(VfsUtilCore.VFS_SEPARATOR_CHAR, newName, newName)

    val taskFiles = task.taskFiles

    fun rename(oldPath: String, newPath: String) {
      val taskFile = taskFiles.remove(oldPath) ?: return
      taskFile.name = newPath
      task.addTaskFile(taskFile)
    }

    if (event.file.isDirectory) {
      val changedPaths = taskFiles.keys.filter { it.startsWith(oldPath) }
      for (oldObjectPath in changedPaths) {
        val newObjectPath = oldObjectPath.replaceFirst(oldPath, newPath)
        rename(oldObjectPath, newObjectPath)
      }
    } else {
      rename(oldPath, newPath)
    }

    YamlFormatSynchronizer.saveItem(task)
  }

  override fun beforeFileMovement(event: VirtualFileMoveEvent) {
    val (task, oldPath) = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    val (oldParentTask, oldParentPath) = event.oldParent.directoryFileInfo(project) ?: return
    val (newParentTask, newParentPath) = event.newParent.directoryFileInfo(project) ?: return

    if (oldParentTask != newParentTask) {
      LOG.warn("Unsupported case. `EduMoveDelegate` should forbid file moving between different tasks")
      return
    }

    val affectedFiles = mutableListOf<TaskFile>()
    val oldPaths = task.taskFiles.keys.filter { it.startsWith(oldPath) }

    for (path in oldPaths) {
      val taskFile = task.taskFiles.remove(path) ?: continue
      PlaceholderPainter.hidePlaceholders(taskFile)
      affectedFiles += taskFile
    }

    for (taskFile in affectedFiles) {
      var newPath = taskFile.name.removePrefix("$oldParentPath/")
      if (newParentPath.isNotEmpty()) {
        newPath = "$newParentPath/$newPath"
      }

      taskFile.name = newPath
      task.addTaskFile(taskFile)
    }

    YamlFormatSynchronizer.saveItem(task)
  }

  /**
   * Handles move events for non course files like drag & drop action produces
   */
  override fun fileMoved(event: VirtualFileMoveEvent) {
    val movedFile = event.file
    val fileInfo = movedFile.fileInfo(project) as? FileInfo.FileInTask ?: return
    val directoryInfo = event.oldParent.directoryFileInfo(project)
    // not null directoryInfo means that we've already processed this file in `beforeFileMovement`
    if (directoryInfo != null) return

    if (movedFile.isDirectory) {
      // We need to collect all children files manually
      // because the platform produces move event only for root file
      VfsUtil.visitChildrenRecursively(movedFile, object : VirtualFileVisitor<Any>(NO_FOLLOW_SYMLINKS) {
        override fun visitFile(file: VirtualFile): Boolean {
          if (!file.isDirectory) {
            val relativePath = VfsUtil.findRelativePath(movedFile, file, VfsUtilCore.VFS_SEPARATOR_CHAR)
            val newFileInfo = fileInfo.copy(pathInTask = "${fileInfo.pathInTask}${VfsUtilCore.VFS_SEPARATOR_CHAR}$relativePath")
            fileInTaskCreated(newFileInfo, file)
          }
          return true
        }
      })
    } else {
      fileInTaskCreated(fileInfo, movedFile)
    }
  }

  private fun VirtualFile.directoryFileInfo(project: Project): FileInfo.FileInTask? {
    val info = fileInfo(project) ?: return null
    return when (info) {
      is FileInfo.TaskDirectory -> FileInfo.FileInTask(info.task, "")
      is FileInfo.FileInTask -> FileInfo.FileInTask(info.task, info.pathInTask)
      else -> null
    }
  }

  protected open fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {}

  companion object {
    private val LOG: Logger = Logger.getInstance(EduVirtualFileListener::class.java)
  }
}
