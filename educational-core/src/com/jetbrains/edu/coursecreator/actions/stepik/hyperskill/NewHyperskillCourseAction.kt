package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreActionBundle
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

@Suppress("ComponentNotRegistered") // Hyperskill.xml
class NewHyperskillCourseAction : DumbAwareAction(
  EduCoreActionBundle.lazyMessage("create.course", HYPERSKILL),
  EduCoreActionBundle.lazyMessage("create.course.description", HYPERSKILL),
  null
) {

  override fun actionPerformed(e: AnActionEvent) {
    CCNewCourseDialog(
      EduCoreActionBundle.message("create.course", HYPERSKILL),
      EduCoreBundle.message("label.create"),
      courseProducer = ::HyperskillCourse
    ).show()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = CCPluginToggleAction.isCourseCreatorFeaturesEnabled
                                         && isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)
  }
}
