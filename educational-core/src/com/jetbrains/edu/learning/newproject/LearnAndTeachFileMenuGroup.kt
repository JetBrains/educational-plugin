package com.jetbrains.edu.learning.newproject

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.messages.EduCoreBundle

class LearnAndTeachFileMenuGroup: DefaultActionGroup(), DumbAware {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = !RemoteEnvHelper.isRemoteDevServer()
    e.presentation.icon = EducationalCoreIcons.CourseAction
    e.presentation.text = EduCoreBundle.message("action.learn.text")
    if (CCPluginToggleAction.isCourseCreatorFeaturesEnabled) {
      e.presentation.text = EduCoreBundle.message("action.learn.and.teach.text")
    }
  }
}