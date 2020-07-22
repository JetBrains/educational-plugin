package com.jetbrains.edu.learning.handlers.rename

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem

class SectionRenameProcessor : EduStudyItemRenameProcessor() {
  override fun getStudyItem(project: Project, course: Course, file: VirtualFile): StudyItem? {
    return EduUtils.getSection(project, course, file)
  }
}
