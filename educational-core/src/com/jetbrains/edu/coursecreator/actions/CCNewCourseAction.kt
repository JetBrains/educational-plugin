package com.jetbrains.edu.coursecreator.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction.COURSE_CREATOR_ENABLED
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog

class CCNewCourseAction : AnAction("Create New Course", "Create new educational course", AllIcons.Actions.Stub) {

  override fun actionPerformed(e: AnActionEvent) {
    CCNewCourseDialog().show()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = PropertiesComponent.getInstance().getBoolean(COURSE_CREATOR_ENABLED)
  }
}
