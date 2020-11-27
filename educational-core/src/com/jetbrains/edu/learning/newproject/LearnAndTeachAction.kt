package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.wm.impl.welcomeScreen.WelcomePopupAction
import com.jetbrains.edu.coursecreator.actions.CCNewCourseAction
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.NewHyperskillCourseAction
import com.jetbrains.edu.learning.codeforces.StartCodeforcesContestAction
import com.jetbrains.edu.learning.newproject.ui.isLearnAndTeachVisible
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectAction
import icons.EducationalCoreIcons

// BACKCOMPAT: 2020.1, 2020.2
class LearnAndTeachAction : WelcomePopupAction() {
  override fun getCaption() = null

  override fun isSilentlyChooseSingleOption() = true

  override fun fillActions(group: DefaultActionGroup) {
    group.addAll(BrowseCoursesAction(), HyperskillProjectAction(), CCNewCourseAction(), NewHyperskillCourseAction(),
                 StartCodeforcesContestAction())
  }

  override fun getTextForEmpty(): String {
    return "no actions available"
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isLearnAndTeachVisible()
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