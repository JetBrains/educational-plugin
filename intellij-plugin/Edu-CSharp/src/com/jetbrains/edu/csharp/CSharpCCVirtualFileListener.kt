package com.jetbrains.edu.csharp

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.learning.getLesson
import com.jetbrains.edu.learning.getTask
import com.jetbrains.rdclient.util.idea.toIOFile

class CSharpCCVirtualFileListener(project: Project, disposable: Disposable) : CCVirtualFileListener(project, disposable) {
  override fun after(events: List<VFileEvent>) {
    val newFilesToIndex = events.filterIsInstance<VFileCreateEvent>()
      .filter { event -> event.file != null && event.parent.path == project.basePath }
      .mapNotNull { it.file?.toIOFile() }

    // is needed to trigger indexing for top-level files and directories in the project for them to appear in the courseView.
    if (newFilesToIndex.isNotEmpty()) {
      CSharpBackendService.getInstance(project).startIndexingTopLevelFiles(newFilesToIndex)
    }

    val propertyChangeEvents = events.filterIsInstance<VFilePropertyChangeEvent>().map { it.file }

    val renamedFilesToIndex = propertyChangeEvents
      .filter { file -> file.parent.path == project.basePath }
      .map { it.toIOFile() }

    // is needed to trigger indexing for top-level files and directories in the project for them to appear in the courseView.
    if (renamedFilesToIndex.isNotEmpty()) {
      CSharpBackendService.getInstance(project).startIndexingTopLevelFiles(renamedFilesToIndex)
    }

    // Tasks have been moved directly
    val movedTasks = events.filterIsInstance<VFileMoveEvent>().mapNotNull { it.file.getTask(project) }
    val tasks = movedTasks.ifEmpty {
      // Tasks inside lessons have been moved
      events.filterIsInstance<VFileMoveEvent>().mapNotNull { it.file.getLesson(project) }.flatMap { it.taskList }
    }
    if (tasks.isNotEmpty()) {
      CSharpBackendService.getInstance(project).addCSProjectFileToSolution(tasks)
    }
    /** add projects on rename (for some reason, tasks affected by rename cannot be not retrieved here, only works when
     * [project.solution.riderSolutionLifecycle.isProjectModelReady] = true, so for now moved to [com.jetbrains.edu.csharp.CSharpBackendService])
     */
    if (propertyChangeEvents.isNotEmpty()) {
      CSharpBackendService.getInstance(project).waitForTasksAndAdd(propertyChangeEvents)
    }
    super.after(events)
  }
}