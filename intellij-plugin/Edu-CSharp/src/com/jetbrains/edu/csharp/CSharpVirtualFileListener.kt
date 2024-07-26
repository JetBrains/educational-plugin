package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.jetbrains.rdclient.util.idea.toIOFile

class CSharpVirtualFileListener(private val project: Project) : BulkFileListener {
  override fun after(events: List<VFileEvent>) {
    val newFilesToIndex = events.filterIsInstance<VFileCreateEvent>()
      .filter { event -> event.file != null && event.parent.path == project.basePath }
      .mapNotNull { it.file?.toIOFile() }

    // is needed to trigger indexing for top-level files and directories in the project for them to appear in the courseView
    if (newFilesToIndex.isNotEmpty()) {
      CSharpBackendService.getInstance(project).startIndexingTopLevelFiles(newFilesToIndex)
    }
    super.after(events)
  }
}