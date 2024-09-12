package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.getStudyItem
import com.jetbrains.rdclient.util.idea.toIOFile

class CSharpVirtualFileListener(private val project: Project) : BulkFileListener {
  override fun after(events: List<VFileEvent>) {
    for (event in events) {
      when (event) {
        is VFileCreateEvent -> fileCreated(event)
        is VFileMoveEvent -> fileMoved(event)
        is VFilePropertyChangeEvent -> propertyChanged(event)
      }
    }
  }

  private fun fileCreated(event: VFileCreateEvent) {
    val file = event.file?.toIOFile() ?: return
    if (event.parent.path == project.basePath) {
      // is needed to trigger indexing for top-level files and directories in the project for them to appear in the courseView
      CSharpBackendService.getInstance(project).includeFilesToCourseView(listOf(file))
    }
    else if (file.isDirectory && (file.name == CSharpConfigurator.OBJ_DIRECTORY || file.name == CSharpConfigurator.BIN_DIRECTORY)) {
      CSharpBackendService.getInstance(project).excludeFilesFromCourseView(listOf(file))
    }
  }

  private fun fileMoved(event: VFileMoveEvent) {
    val file = event.file.toIOFile()
    if (event.newParent.path == project.basePath) {
      // is needed to trigger indexing for top-level files and directories in the project for them to appear in the courseView
      CSharpBackendService.getInstance(project).includeFilesToCourseView(listOf(file))
    }
    if (event.file.getStudyItem(project) != null) {
      project.course?.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
    }
  }

  private fun propertyChanged(propertyChangeEvent: VFilePropertyChangeEvent) {
    val file = propertyChangeEvent.file
    if (file.parent.path == project.basePath) {
      CSharpBackendService.getInstance(project).excludeFilesFromCourseView(listOf(propertyChangeEvent.oldPath.toIOFile()))
      CSharpBackendService.getInstance(project).includeFilesToCourseView(listOf(file.toIOFile()))
    }
  }
}