package com.jetbrains.edu.coursecreator.actions.studio

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCVirtualFileListener
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem


class CCDeleteLesson : DeleteStudyItem("Delete Lesson") {
  override fun getStudyItem(project: Project, file: VirtualFile): StudyItem? {
    val course = StudyTaskManager.getInstance(project).course?:return null
    return course.getLesson(file.name)
  }

  override fun deleteItem(course: Course, file: VirtualFile, project: Project) = CCVirtualFileListener.deleteLesson(course, file, project)
}