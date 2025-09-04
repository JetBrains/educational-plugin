package com.jetbrains.edu.coursecreator.actions.taskFile

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.configuration.ArchiveInclusionPolicy
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.courseFileAttributes
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getAdditionalFile
import com.jetbrains.edu.learning.courseFormat.ext.pathInCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isToEncodeContent
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class CCIncludeIntoArchive : CCChangeFilePropertyActionBase(EduCoreBundle.lazyMessage("action.include.into.archive.title")) {

  override fun isAvailableForSingleFile(project: Project, task: Task?, file: VirtualFile): Boolean {
    if (task != null) return false
    val path = file.pathInCourse(project) ?: return false
    val course = project.course ?: return false
    val configurator = course.configurator ?: return false
    val courseFileAttributes = configurator.courseFileAttributes(project, file)
    if (course.getAdditionalFile(path) != null) return false
    if (courseFileAttributes.archiveInclusionPolicy == ArchiveInclusionPolicy.MUST_EXCLUDE) return false

    return true
  }

  override fun isAvailableForDirectory(project: Project, task: Task?, directory: VirtualFile): Boolean {
    return task == null
  }

  override fun createStateForFile(project: Project, course: Course, configurator: EduConfigurator<*>, task: Task?, file: VirtualFile): State? {
    if (task != null) return null
    val path = file.pathInCourse(project) ?: return null
    if (course.getAdditionalFile(path) != null) return null
    val courseFileAttributes = configurator.courseFileAttributes(project, file)
    if (courseFileAttributes.archiveInclusionPolicy == ArchiveInclusionPolicy.MUST_EXCLUDE) return null
    return IncludeFileIntoArchive(course, file, path)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.IncludeIntoArchive"
  }
}

private class IncludeFileIntoArchive(private val course: Course, private val file: VirtualFile, private val path: String) : State {

  override fun changeState(project: Project) {
    course.additionalFiles += EduFile(path, if (file.isToEncodeContent) {
      BinaryContents.EMPTY
    }
    else {
      TextualContents.EMPTY
    })
  }

  override fun restoreState(project: Project) {
    course.additionalFiles = course.additionalFiles.filter { it.name != path }
  }
}
