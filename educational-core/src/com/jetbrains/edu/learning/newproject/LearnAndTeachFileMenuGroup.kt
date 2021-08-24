package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction

class LearnAndTeachFileMenuGroup: DefaultActionGroup(), DumbAware {
  override fun update(e: AnActionEvent) {
    e.presentation.icon = EducationalCoreIcons.CourseAction
    e.presentation.text = "Learn"
    if (CCPluginToggleAction.isCourseCreatorFeaturesEnabled) {
      e.presentation.text += " and Teach"
    }
  }
}