package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import javax.swing.Icon

abstract class SyncCourseAction(text: String, description: String, icon: Icon?) : DumbAwareAction(text, description, icon) {

  constructor(text: String): this(text, text, null)

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) return
    synchronizeCourse(project)
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!isAvailable(project)) return

    presentation.isEnabledAndVisible = true
  }

  abstract fun synchronizeCourse(project: Project)

  abstract fun isAvailable(project: Project): Boolean
}