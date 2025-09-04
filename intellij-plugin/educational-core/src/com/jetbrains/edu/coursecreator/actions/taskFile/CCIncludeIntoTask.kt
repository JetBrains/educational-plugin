package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.FileInfo
import com.jetbrains.edu.learning.canBeAddedToTask
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.fileInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class CCIncludeIntoTask : CCChangeFilePropertyActionBase(EduCoreBundle.lazyMessage("action.include.into.task.title")) {

  override fun isAvailableForSingleFile(project: Project, task: Task?, file: VirtualFile): Boolean =
    if (task == null) {
      false
    }
    else {
      file.canBeAddedToTask(project)
    }

  override fun isAvailableForDirectory(project: Project, task: Task?, directory: VirtualFile): Boolean {
    return task != null
  }

  override fun createStateForFile(project: Project, course: Course, configurator: EduConfigurator<*>, task: Task?, file: VirtualFile): State? {
    if (task == null) return null
    if (!file.canBeAddedToTask(project)) return null
    val info = file.fileInfo(project) as? FileInfo.FileInTask ?: return null
    return IncludeFileIntoTask(info)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.IncludeIntoTask"
  }
}

private class IncludeFileIntoTask(private val info: FileInfo.FileInTask) : State {

  override fun changeState(project: Project) {
    info.task.addTaskFile(info.pathInTask)
  }

  override fun restoreState(project: Project) {
    info.task.removeTaskFile(info.pathInTask)
  }
}
