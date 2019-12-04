package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

@Suppress("ComponentNotRegistered") // Hyperskill.xml
class NewHyperskillCourseAction : DumbAwareAction("Create New Hyperskill Course", "Create new hyperskill course", null) {

  override fun actionPerformed(e: AnActionEvent) {
    CCNewCourseDialog("Create Hyperskill Course", "Create", courseProducer = ::HyperskillCourse).show()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = CCPluginToggleAction.isCourseCreatorFeaturesEnabled
                                         && isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)
  }
}
