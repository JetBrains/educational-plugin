package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomePopupAction
import com.jetbrains.edu.coursecreator.actions.CCNewCourseAction
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.coursera.ImportLocalCourseAction
import com.jetbrains.edu.learning.coursera.StartCourseraProgrammingAssignment
import com.jetbrains.edu.learning.stepik.actions.StartStepikCourseAction
import icons.EducationalCoreIcons

class LearnAndTeachAction : WelcomePopupAction() {
  override fun getCaption() = null

  override fun isSilentlyChooseSingleOption() = true

  override fun fillActions(group: DefaultActionGroup) {
    group.addAll(BrowseCoursesAction(), ImportLocalCourseAction(),
                 StartCourseraProgrammingAssignment(), StartStepikCourseAction(),
                 CCNewCourseAction())
  }

  override fun getTextForEmpty(): String {
    return "no actions available"
  }

  override fun update(e: AnActionEvent) {
    e.presentation.icon = EducationalCoreIcons.CourseAction
    e.presentation.text = "Learn"
    if (CCPluginToggleAction.isCourseCreatorFeaturesEnabled) {
      e.presentation.text += " and Teach"
    }
  }
}