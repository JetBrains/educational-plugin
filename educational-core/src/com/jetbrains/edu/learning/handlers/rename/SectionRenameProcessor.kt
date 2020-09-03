package com.jetbrains.edu.learning.handlers.rename

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.getSection

class SectionRenameProcessor : EduStudyItemRenameProcessor() {
  override fun getStudyItem(project: Project, course: Course, file: VirtualFile): StudyItem? {
    return file.getSection(project)
  }
}
