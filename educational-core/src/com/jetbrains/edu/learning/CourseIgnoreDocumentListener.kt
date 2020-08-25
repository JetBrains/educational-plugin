package com.jetbrains.edu.learning

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils

class CourseIgnoreDocumentListener(project: Project) : EduDocumentListenerBase(project) {

  override fun documentChanged(event: DocumentEvent) {
    if (!event.isInProjectContent()) return
    val file = fileDocumentManager.getFile(event.document) ?: return
    if (file.name != EduNames.COURSE_IGNORE) return
    if (!CCUtils.isCourseCreator(project)) return
    ProjectView.getInstance(project).refresh()
  }

}
