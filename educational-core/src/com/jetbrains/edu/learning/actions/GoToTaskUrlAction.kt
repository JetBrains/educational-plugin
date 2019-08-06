package com.jetbrains.edu.learning.actions

import com.google.common.annotations.VisibleForTesting
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.GO_TO_CODEFORCES_ACTION
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.FeedbackLink.LinkType.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.getStepikLink
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import icons.EducationalCoreIcons

private const val LEAVE_A_COMMENT_ACTION = "Leave a comment"

class GoToTaskUrlAction : DumbAwareAction(LEAVE_A_COMMENT_ACTION, LEAVE_A_COMMENT_ACTION, EducationalCoreIcons.CommentTask),
                          RightAlignedToolbarAction {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = EduUtils.getCurrentTask(project) ?: return
    val link = getLink(task)
    BrowserUtil.browse(link)
    EduCounterUsageCollector.leaveFeedback()
  }

  override fun update(e: AnActionEvent) {
    templatePresentation.isEnabledAndVisible = false

    val project = e.project ?: return
    if (!EduUtils.isStudentProject(project)) {
      return
    }

    val task = EduUtils.getCurrentTask(project) ?: return
    if (task is CodeforcesTask) {
      templatePresentation.text = GO_TO_CODEFORCES_ACTION
      templatePresentation.icon = EducationalCoreIcons.CodeforcesGrayed
    }
    val feedbackLink = task.feedbackLink
    val course = task.course
    templatePresentation.isEnabledAndVisible = when (feedbackLink.type) {
      NONE -> false
      CUSTOM -> feedbackLink.link != null
      STEPIK -> (course is EduCourse && course.isRemote) || course is HyperskillCourse
    }
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