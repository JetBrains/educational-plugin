package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomePopupAction
import com.jetbrains.edu.coursecreator.actions.CCNewCourseAction
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction
import com.jetbrains.edu.learning.coursera.StartCourseraAssignment
import com.jetbrains.edu.learning.stepik.actions.StartStepikCourseAction
import com.jetbrains.edu.learning.stepik.alt.HyperskillProjectAction
import icons.EducationalCoreIcons

class LearnAndTeachAction : WelcomePopupAction() {
  override fun getCaption() = null

  override fun isSilentlyChooseSingleOption() = true

  override fun fillActions(group: DefaultActionGroup) {
    group.addAll(BrowseCoursesAction(), StartCourseraAssignment(), HyperskillProjectAction(),
                 ImportLocalCourseAction(), StartStepikCourseAction(),
                 CCNewCourseAction())
  }

  override fun getTextForEmpty(): String {
    return "no actions available"
  }

  override fun update(e: AnActionEvent) {
    e.presentation.icon = EducationalCoreIcons.CourseAction
    updatePresentationText(e.presentation)
  }

  companion object {
    fun updatePresentationText(presentation: Presentation) {
      presentation.text = "Learn"
      if (CCPluginToggleAction.isCourseCreatorFeaturesEnabled) {
        presentation.text += " and Teach"
      }
    }
  }
}