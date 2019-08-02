package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import javax.swing.Icon

abstract class SyncCourseAction(text: String?, description: String?, icon: Icon?) : AnAction(text, description, icon) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project
    if (project != null) {
      synchronizeCourse(project)
    }
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    val visible = isAvailable(project)
    val presentation = e.presentation
    presentation.isEnabledAndVisible = visible
  }

  abstract fun synchronizeCourse(project: Project)

  abstract fun isAvailable(project: Project?): Boolean
}