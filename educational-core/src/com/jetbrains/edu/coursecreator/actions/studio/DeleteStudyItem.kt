package com.jetbrains.edu.coursecreator.actions.studio

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem

abstract class DeleteStudyItem(text: String) : DumbAwareAction(text) {
  override fun actionPerformed(e: AnActionEvent?) {
    val dataContext = e?.dataContext!!
    val project = e.project!!
    val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext)!!
    val course = StudyTaskManager.getInstance(project).course!!
    deleteItem(course, virtualFile, project)
    ApplicationManager.getApplication().runWriteAction({ virtualFile.delete(DeleteStudyItem::class.java) })
  }

  override fun update(e: AnActionEvent?) {
    e?:return
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val dataContext = e.dataContext
    val project = e.project?:return
    if (CCUtils.isCourseCreator(project).not()) {
      return
    }
    val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext)?:return
    presentation.isEnabledAndVisible = getStudyItem(project, virtualFile) != null
  }

  abstract fun getStudyItem(project: Project, file: VirtualFile): StudyItem?

  abstract fun deleteItem(course: Course, file: VirtualFile, project: Project)
}