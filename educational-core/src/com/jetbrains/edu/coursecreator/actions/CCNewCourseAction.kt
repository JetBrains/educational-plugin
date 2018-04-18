package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import icons.EducationalCoreIcons

class CCNewCourseAction : AnAction("Create New Course", "Create new educational course", EducationalCoreIcons.CourseAction) {

  override fun actionPerformed(e: AnActionEvent) {
    CCNewCourseDialog("Create Course", "Create").show()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = CCPluginToggleAction.isCourseCreatorFeaturesEnabled
  }
}
