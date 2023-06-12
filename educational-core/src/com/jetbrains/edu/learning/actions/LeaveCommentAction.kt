package com.jetbrains.edu.learning.actions

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.getStepikLink
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class LeaveCommentAction : DumbAwareAction(EduCoreBundle.lazyMessage("action.leave.comment.text"),
                                           EduCoreBundle.lazyMessage("action.leave.comment.text"), EducationalCoreIcons.CommentTask),
                           RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    val link = getLink(task) ?: error("LeaveFeedbackAction is not supported")
    EduBrowser.getInstance().browse(link)
    EduCounterUsageCollector.leaveFeedback()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!project.isStudentProject()) return
    val task = project.getCurrentTask() ?: return
    val course = task.course

    if (course is HyperskillCourse) {
      e.presentation.text = EduCoreBundle.message("action.show.discussions.text")
      e.presentation.description = EduCoreBundle.message("action.show.discussions.description")
      addSynonym(EduCoreBundle.lazyMessage("action.show.discussions.text"))
    }

    e.presentation.isEnabledAndVisible = getLink(task) != null
  }

  companion object {
    const val ACTION_ID: String = "Educational.LeaveCommentAction"

    @VisibleForTesting
    fun getLink(task: Task): String? {
      val feedbackLink = task.feedbackLink
      val course = task.course
      val courseLink = course.rateOnMarketplaceLink
      return when {
        feedbackLink != null -> feedbackLink
        courseLink != null -> courseLink
        course is EduCourse && course.isStepikRemote -> getStepikLink(task, task.lesson)
        else -> null
      }
    }
  }
}