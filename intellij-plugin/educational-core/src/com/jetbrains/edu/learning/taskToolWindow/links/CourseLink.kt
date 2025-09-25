package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.navigation.ParsedInCourseLink
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.LinkType.IN_COURSE

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class CourseLink(link: String) : TaskDescriptionLink<ParsedInCourseLink<*>, ParsedInCourseLink<*>?>(link, IN_COURSE) {

  override fun resolve(project: Project): ParsedInCourseLink<*>? = ParsedInCourseLink.parse(project, linkPath)

  override fun open(project: Project, parsedLink: ParsedInCourseLink<*>) {
    runInEdt {
      when (parsedLink) {
        is ParsedInCourseLink.ItemContainerDirectory -> OpenFileDescriptor(project, parsedLink.file).navigate(true)
        is ParsedInCourseLink.TaskDirectory -> NavigationUtils.navigateToTask(project, parsedLink.item, closeOpenedFiles = false)
        is ParsedInCourseLink.FileInTask -> NavigationUtils.navigateToTask(
          project,
          parsedLink.item,
          closeOpenedFiles = false,
          fileToActivate = parsedLink.file
        )
        is ParsedInCourseLink.CourseAdditionalFile -> {
          // open a file editor, and if failed (an unknown file type, for example), open it in the project view
          val editors = FileEditorManager.getInstance(project).openFile(parsedLink.file, true)
          if (editors.isEmpty()) {
            ProjectView.getInstance(project).select(null, parsedLink.file, true)
          }
        }
      }
    }
  }

  override suspend fun validate(project: Project, parsedLink: ParsedInCourseLink<*>?): String? {
    return if (parsedLink == null) "Failed to find an item in course by `$linkPath` path" else null
  }
}
