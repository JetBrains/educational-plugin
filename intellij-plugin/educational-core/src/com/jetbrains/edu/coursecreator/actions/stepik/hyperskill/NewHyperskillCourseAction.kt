package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class NewHyperskillCourseAction : DumbAwareAction() {

 override fun actionPerformed(e: AnActionEvent) {
    CCSettings.getInstance().useHtmlAsDefaultTaskFormat = true
    CCNewCourseDialog(
      EduCoreBundle.message("action.Educational.Educator.NewHyperskillCourse.text"),
      EduCoreBundle.message("label.create"),
      courseProducer = ::HyperskillCourse
    ).show()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    if (!isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)) return
    e.presentation.isEnabledAndVisible = true
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.NewHyperskillCourse"
  }
}
