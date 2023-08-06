package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.settings.CCSettings
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import org.jetbrains.annotations.NonNls

@Suppress("ComponentNotRegistered") // Hyperskill.xml
class NewHyperskillCourseAction : DumbAwareAction(
  EduCoreBundle.lazyMessage("action.create.course.text", HYPERSKILL),
  EduCoreBundle.lazyMessage("action.create.course.description", HYPERSKILL),
  null
) {

  override fun actionPerformed(e: AnActionEvent) {
    CCSettings.getInstance().useHtmlAsDefaultTaskFormat = true
    CCNewCourseDialog(
      EduCoreBundle.message("action.create.course.text", HYPERSKILL),
      EduCoreBundle.message("label.create"),
      courseProducer = ::HyperskillCourse
    ).show()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    if (!isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)) return
    if (RemoteEnvHelper.isRemoteDevServer()) return
    e.presentation.isEnabledAndVisible = true
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.NewHyperskillCourse"
  }
}
