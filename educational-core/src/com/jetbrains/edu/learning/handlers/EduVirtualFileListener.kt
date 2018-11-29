package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.fileInfo

abstract class EduVirtualFileListener(protected val project: Project) : VirtualFileListener {

  override fun fileCreated(event: VirtualFileEvent) {
    if (event.file.isDirectory) return
    val fileInfo = event.file.fileInfo(project) as? FileInfo.FileInTask ?: return
    fileInTaskCreated(fileInfo)
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
  protected open fun fileInTaskCreated(fileInfo: FileInfo.FileInTask) {
    val (task, pathInTask) = fileInfo
    if (task.getTaskFile(pathInTask) == null) {
      val taskFile = task.addTaskFile(pathInTask)
      if (EduUtils.isStudentProject(project)) {
        taskFile.isUserCreated = true
      }
    }
  }
}
