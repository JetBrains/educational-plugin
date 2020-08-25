package com.jetbrains.edu.learning.actions

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.components.labels.ActionLink
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FeedbackLink.LinkType.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.getStepikLink
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import icons.EducationalCoreIcons

private const val GO_TO_TASK_URL_ACTION = "Leave a comment"

class GoToTaskUrlAction : DumbAwareAction(GO_TO_TASK_URL_ACTION, GO_TO_TASK_URL_ACTION, EducationalCoreIcons.CommentTask),
                          RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = EduUtils.getCurrentTask(project) ?: return
    val link = getLink(task)
    BrowserUtil.browse(link)
    when {
      task is CodeforcesTask -> {
        EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.CODEFORCES)
      }
      task.course is HyperskillCourse && isOpenOnJBAAction(e) -> {
        EduCounterUsageCollector.linkClicked(EduCounterUsageCollector.LinkType.JBA)
      }
      else -> {
        EduCounterUsageCollector.leaveFeedback()
      }
    }
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) {
      return
    }

    val task = EduUtils.getCurrentTask(project) ?: return

    val feedbackLink = task.feedbackLink
    val course = task.course
    e.presentation.isEnabledAndVisible = when (feedbackLink.type) {
      NONE -> false
      CUSTOM -> feedbackLink.link != null
      STEPIK -> (course is EduCourse && course.isRemote) || course is HyperskillCourse
    }
  }

  private fun isOpenOnJBAAction(e: AnActionEvent): Boolean {
    val link = e.dataContext.getData("contextComponent") as? ActionLink ?: return false
    return link.text == EduCoreBundle.message("action.open.on.text", EduNames.JBA)
  }

  companion object {
    const val ACTION_ID: String = "Educational.GoToTaskUrlAction"

    @JvmStatic
    @VisibleForTesting
    fun getLink(task: Task): String {
      val feedbackLink = task.feedbackLink
      return when (feedbackLink.type) {
        NONE -> error("GoToTaskUrlAction should be disabled for NONE links")
        CUSTOM -> feedbackLink.link ?: error("Custom link doesn't contain an actual link")
        STEPIK -> getStepikLink(task, task.lesson)
      }
    }
  }
}