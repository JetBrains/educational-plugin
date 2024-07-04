package com.jetbrains.edu.learning.marketplace.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.annotations.NonNls

@Suppress("DialogTitleCapitalization")
class RateMarketplaceCourseAction : DumbAwareAction(
  EduCoreBundle.lazyMessage("marketplace.action.rate.course"),
  EduCoreBundle.lazyMessage("marketplace.action.rate.course"),
  EducationalCoreIcons.RateCourse
), RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val course = project.course ?: return
    val link = course.feedbackLink ?: error("RateMarketplaceCourseAction is not supported")
    EduBrowser.getInstance().browse(link)
    EduCounterUsageCollector.rateMarketplaceCourse()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!project.isStudentProject()) return
    val course = project.course as? EduCourse ?: return
    if (!course.isMarketplaceRemote) return

    e.presentation.isEnabledAndVisible = course.feedbackLink != null
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.RateMarketplaceCourseAction"
  }
}