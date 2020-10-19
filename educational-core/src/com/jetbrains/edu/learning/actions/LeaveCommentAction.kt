package com.jetbrains.edu.learning.actions

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.getStepikLink
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import icons.EducationalCoreIcons

private const val LEAVE_A_COMMENT_ACTION = "Leave a comment"
private const val SHOW_DISCUSSIONS_ACTION = "Show discussions"

@Suppress("ComponentNotRegistered")
class LeaveCommentAction : DumbAwareAction(LEAVE_A_COMMENT_ACTION, LEAVE_A_COMMENT_ACTION, EducationalCoreIcons.CommentTask),
                           RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = EduUtils.getCurrentTask(project) ?: return
    val link = getLink(task)
    EduBrowser.browse(link)
    EduCounterUsageCollector.leaveFeedback()
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) return
    val task = EduUtils.getCurrentTask(project) ?: return
    val course = task.course

    if (course is HyperskillCourse) {
      e.presentation.text = SHOW_DISCUSSIONS_ACTION
      e.presentation.description = SHOW_DISCUSSIONS_ACTION
      addSynonym(SHOW_DISCUSSIONS_ACTION)
    }

    val feedbackLink = task.feedbackLink
    e.presentation.isEnabledAndVisible = when (feedbackLink.type) {
      FeedbackLink.LinkType.NONE -> false
      FeedbackLink.LinkType.CUSTOM -> feedbackLink.link != null
      FeedbackLink.LinkType.STEPIK -> course is EduCourse && course.isRemote
    }
  }

  companion object {
    const val ACTION_ID: String = "Educational.LeaveCommentAction"

    @JvmStatic
    @VisibleForTesting
    fun getLink(task: Task): String {
      val feedbackLink = task.feedbackLink
      return when (feedbackLink.type) {
        FeedbackLink.LinkType.NONE -> error("LeaveFeedbackAction should be disabled for NONE links")
        FeedbackLink.LinkType.CUSTOM -> feedbackLink.link ?: error("Custom link doesn't contain an actual link")
        FeedbackLink.LinkType.STEPIK -> getStepikLink(task, task.lesson)
      }
    }
  }
}