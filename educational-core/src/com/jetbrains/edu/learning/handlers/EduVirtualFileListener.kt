package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.*
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.fileInfo

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

  protected open fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {}
}
