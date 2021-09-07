package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduExperimentalFeatures
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
    CCNewCourseDialog(
      EduCoreBundle.message("action.create.course.text", HYPERSKILL),
      EduCoreBundle.message("label.create"),
      courseProducer = ::HyperskillCourse
    ).show()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = CCPluginToggleAction.isCourseCreatorFeaturesEnabled
                                         && isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)
  }

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.Educator.NewHyperskillCourse"
  }
}
