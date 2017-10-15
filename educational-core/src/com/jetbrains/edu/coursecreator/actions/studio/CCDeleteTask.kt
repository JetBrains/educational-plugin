package com.jetbrains.edu.coursecreator.actions.studio

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCVirtualFileListener
import com.jetbrains.edu.learning.StudyUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem

class CCDeleteTask : DeleteStudyItem("Delete Task") {
  override fun getStudyItem(project: Project, file: VirtualFile): StudyItem? = StudyUtils.getTask(project, file)

  override fun deleteItem(course: Course, file: VirtualFile, project: Project) = CCVirtualFileListener.deleteTask(course, file)
}