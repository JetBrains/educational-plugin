package com.jetbrains.edu.coursecreator.actions.studio

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.StudyItem

class CCDeleteTask : CCDeleteStudyItem("Delete Task") {
  override fun getStudyItem(project: Project, file: VirtualFile): StudyItem? = EduUtils.getTaskForFile(project, file)
}