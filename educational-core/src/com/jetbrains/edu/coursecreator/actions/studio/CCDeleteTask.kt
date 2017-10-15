package com.jetbrains.edu.coursecreator.actions.studio

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.CCVirtualFileListener
import com.jetbrains.edu.learning.StudyUtils

class CCDeleteTask : DumbAwareAction("Delete Task") {
  override fun actionPerformed(e: AnActionEvent?) {
    val dataContext = e?.dataContext!!
    val project = e.project
    val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext)!!
    val task = StudyUtils.getTask(project!!, virtualFile)!!
    CCVirtualFileListener.deleteTask(task.lesson.course, virtualFile)
    ApplicationManager.getApplication().runWriteAction({ virtualFile.delete(CCDeleteTask::class.java) })
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
    StudyUtils.getTask(project, virtualFile)?:return
    presentation.isEnabledAndVisible = true
  }
}