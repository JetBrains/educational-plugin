package com.jetbrains.edu.learning.marketplace.course

import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.StartCourseAction
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.stepik.course.CourseConnector
import com.jetbrains.edu.learning.stepik.course.ImportCourseDialog

@Suppress("ComponentNotRegistered")
class StartMarketplaceCourseAction : StartCourseAction(MARKETPLACE) {
  override val dialog: ImportCourseDialog
    get() = ImportMarketplaceCourseDialog(courseConnector)
  override val courseConnector: CourseConnector = MarketplaceConnector.getInstance()

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE)
    super.update(e)
  }
}