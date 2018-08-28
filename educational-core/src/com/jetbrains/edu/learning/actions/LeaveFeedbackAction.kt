package com.jetbrains.edu.learning.actions

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse
import com.jetbrains.edu.learning.stepik.courseFormat.ext.id
import com.jetbrains.edu.learning.stepik.courseFormat.ext.isAdaptive
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import icons.EducationalCoreIcons

private const val ACTION_TEXT = "Leave a comment"

class LeaveFeedbackAction : DumbAwareAction(ACTION_TEXT, ACTION_TEXT, EducationalCoreIcons.CommentTask), RightAlignedToolbarAction {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = EduUtils.getCurrentTask(project) ?: return
    val link = getLink(task)
    BrowserUtil.browse(link)
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) {
      return
    }
    val task = EduUtils.getCurrentTask(project) ?: return
    val feedbackLink = task.feedbackLink
    presentation.isEnabledAndVisible = when (feedbackLink.type) {
      FeedbackLink.LinkType.NONE -> false
      FeedbackLink.LinkType.CUSTOM -> feedbackLink.link != null
      FeedbackLink.LinkType.STEPIK -> task.course is StepikCourse || task.course is HyperskillCourse
    }
  }

  companion object {
    const val ACTION_ID = "Educational.LeaveFeedback"

    private fun getStepikLink(task: Task): String {
      return "${StepikNames.STEPIK_URL}/lesson/${task.lesson.id}/step/${task.index}"
    }

    @JvmStatic
    @VisibleForTesting
    fun getLink(task: Task): String {
      val feedbackLink = task.feedbackLink
      return when (feedbackLink.type) {
        FeedbackLink.LinkType.NONE -> error("LeaveFeedbackAction should be disabled for NONE links")
        FeedbackLink.LinkType.CUSTOM -> feedbackLink.link ?: error("Custom link doesn't contain an actual link")
        FeedbackLink.LinkType.STEPIK -> getStepikLink(task)
      }
    }
  }
}