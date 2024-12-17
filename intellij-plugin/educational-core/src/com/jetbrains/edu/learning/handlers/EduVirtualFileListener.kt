package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.*
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.fileInfo
import com.jetbrains.edu.learning.placeholder.PlaceholderHighlightingManager
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

abstract class EduVirtualFileListener(protected val project: Project) : BulkFileListener {

  override fun before(events: List<VFileEvent>) {
    for (event in events) {
      when (event) {
        is VFilePropertyChangeEvent -> beforePropertyChange(event)
        is VFileMoveEvent -> beforeFileMovement(event)
        is VFileDeleteEvent -> beforeFileDeletion(event)
      }
    }
  }

  override fun after(events: List<VFileEvent>) {
    val configEvents = events.filter { it.file != null && YamlFormatSynchronizer.isLocalConfigFile(it.file!!) }
    for (event in events - configEvents.toSet()) {
      when (event) {
        is VFileCreateEvent -> event.file?.let { fileCreated(it) }
        is VFileCopyEvent -> event.newParent.findChild(event.newChildName)?.let { fileCreated(it) }
        is VFileMoveEvent -> fileMoved(event)
        is VFileDeleteEvent -> fileDeleted(event)
        is VFileContentChangeEvent -> fileChanged(event.file)
      }
    }
    configUpdated(configEvents)
  }

  protected open fun configUpdated(configEvents: List<VFileEvent>) {}

  protected open fun fileCreated(file: VirtualFile) {
    if (file.isDirectory) return
    val fileInfo = file.fileInfo(project) as? FileInfo.FileInTask ?: return
    if (EduUtilsKt.isTaskDescriptionFile(fileInfo.pathInTask)) return
    fileInTaskCreated(fileInfo, file)
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
   * But in tests, course files usually are created by [com.jetbrains.edu.learning.EduTestCase.courseWithFiles] which triggers virtual file listener because
   * sometimes listener is initialized in `[com.jetbrains.edu.learning.EduTestCase.setUp] method and [com.jetbrains.edu.learning.EduTestCase.courseWithFiles] creates course files after it.
   * In such cases, these checks prevent replacing a correct task file
   * with empty (without placeholders, hints, etc.) one.
   */
  private fun fileInTaskCreated(fileInfo: FileInfo.FileInTask, createFile: VirtualFile) {
    val (task, pathInTask) = fileInfo
    if (task.getTaskFile(pathInTask) == null) {
      val taskFile = task.addTaskFile(pathInTask)
      taskFileCreated(taskFile, createFile)
      YamlFormatSynchronizer.saveItem(fileInfo.task)
    }
  }

  protected open fun beforePropertyChange(event: VFilePropertyChangeEvent) {
    if (event.propertyName != VirtualFile.PROP_NAME) return
    val newName = event.newValue as? String ?: return
    val (task, oldPath) = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    val newPath = oldPath.replaceAfterLast(VfsUtilCore.VFS_SEPARATOR_CHAR, newName, newName)

    fun rename(oldPath: String, newPath: String) {
      val taskFileIndex = task.taskFileIndex(oldPath) ?: return
      val taskFile = task.removeTaskFile(oldPath) ?: return
      taskFile.name = newPath
      task.addTaskFile(taskFile, taskFileIndex)
    }

    if (event.file.isDirectory) {
      val changedPaths = task.taskFiles.keys.filter { oldPath.isParentOf(it) }
      for (oldObjectPath in changedPaths) {
        val newObjectPath = oldObjectPath.replaceFirst(oldPath, newPath)
        rename(oldObjectPath, newObjectPath)
      }
    }
    else {
      rename(oldPath, newPath)
    }

    YamlFormatSynchronizer.saveItem(task)
  }

  private fun beforeFileMovement(event: VFileMoveEvent) {
    val (task, oldPath) = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    val (oldParentTask, oldParentPath) = event.oldParent.directoryFileInfo(project) ?: return
    val (newParentTask, newParentPath) = event.newParent.directoryFileInfo(project) ?: return

    if (oldParentTask != newParentTask) {
      LOG.warn("Unsupported case. `EduMoveDelegate` should forbid file moving between different tasks")
      return
    }

    val newTaskFiles = LinkedHashMap<String, TaskFile>()

    for (taskFile in task.taskFiles.values) {
      val path = taskFile.name
      val isAffected = oldPath == path || oldPath.isParentOf(path)

      if (isAffected) {
        PlaceholderHighlightingManager.hidePlaceholders(project, taskFile.answerPlaceholders)
        var newPath = path.removePrefix("$oldParentPath/")
        if (newParentPath.isNotEmpty()) {
          newPath = "$newParentPath/$newPath"
        }

        taskFile.name = newPath
      }

      newTaskFiles[taskFile.name] = taskFile
    }

    task.taskFiles = newTaskFiles

    YamlFormatSynchronizer.saveItem(task)
  }

  /**
   * Handles move events for non course files like drag & drop action produces
   */
  protected open fun fileMoved(event: VFileMoveEvent) {
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
    }
    else {
      fileInTaskCreated(fileInfo, movedFile)
    }
  }

  private fun fileDeleted(event: VFileDeleteEvent) {
    if (event.requestor == null) return
    val fileInfo = event.file.fileInfo(project) ?: return
    fileDeleted(fileInfo, event.file)
  }

  private fun fileChanged(file: VirtualFile) {
    val (task, pathInTask) = file.fileInfo(project) as? FileInfo.FileInTask ?: return
    val taskFile = task.getTaskFile(pathInTask) ?: return
    return taskFileChanged(taskFile, file)
  }

  protected fun VirtualFile.directoryFileInfo(project: Project): FileInfo.FileInTask? {
    val info = fileInfo(project) ?: return null
    return when (info) {
      is FileInfo.TaskDirectory -> FileInfo.FileInTask(info.task, "")
      is FileInfo.FileInTask -> FileInfo.FileInTask(info.task, info.pathInTask)
      else -> null
    }
  }

  protected fun String.isParentOf(child: String) = child.startsWith(this + VfsUtilCore.VFS_SEPARATOR_CHAR)

  protected open fun beforeFileDeletion(event: VFileDeleteEvent) {}
  protected open fun fileDeleted(fileInfo: FileInfo, file: VirtualFile) {}
  protected open fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {}

  protected open fun taskFileChanged(taskFile: TaskFile, file: VirtualFile) {}

  companion object {
    private val LOG: Logger = Logger.getInstance(EduVirtualFileListener::class.java)
  }

}
