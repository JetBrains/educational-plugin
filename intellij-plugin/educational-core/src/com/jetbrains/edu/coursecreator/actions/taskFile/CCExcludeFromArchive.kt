package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.belongsToTask
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.ext.getAdditionalFile
import com.jetbrains.edu.learning.courseFormat.ext.pathInCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class CCExcludeFromArchive : CCChangeFilePropertyActionBase(EduCoreBundle.lazyMessage("action.exclude.from.archive.title")) {

  override fun isAvailableForSingleFile(project: Project, task: Task?, file: VirtualFile): Boolean {
    if (file.belongsToTask(project)) return false
    val path = file.pathInCourse(project) ?: return false
    val course = project.course ?: return false
    return course.getAdditionalFile(path) != null
  }

  override fun isAvailableForDirectory(project: Project, task: Task?, directory: VirtualFile): Boolean {
    return task == null
  }

  override fun createStateForFile(project: Project, course: Course, configurator: EduConfigurator<*>, task: Task?, file: VirtualFile): State? {
    if (file.belongsToTask(project)) return null
    val path = file.pathInCourse(project) ?: return null
    return RemoveFileFromArchive(course, path)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.ExcludeFromArchive"
  }
}

private class RemoveFileFromArchive(private val course: Course, private val path: String) : State {

  private val initialFileWithIndex: IndexedValue<EduFile>? = course.additionalFiles.withIndex().find { it.value.name == path }

  override fun changeState(project: Project) {
    if (initialFileWithIndex == null) return

    course.additionalFiles = course.additionalFiles.filter { it.name != path }
  }

  override fun restoreState(project: Project) {
    if (initialFileWithIndex == null) return
    val (initialIndex, initialFile) = initialFileWithIndex

    course.additionalFiles = ArrayList(course.additionalFiles).apply {
      add(initialIndex, initialFile)
    }
  }
}
