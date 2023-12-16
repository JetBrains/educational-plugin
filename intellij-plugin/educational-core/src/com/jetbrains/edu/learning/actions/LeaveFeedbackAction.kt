package com.jetbrains.edu.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import org.jetbrains.annotations.NonNls

class LeaveFeedbackAction : DumbAwareAction(EduCoreBundle.lazyMessage("action.leave.comment.text"), EduCoreBundle.lazyMessage("action.leave.comment.text"), EducationalCoreIcons.CommentTask), RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    val link = task.feedbackLink ?: error("LeaveFeedbackAction is not supported")
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

    e.presentation.isEnabledAndVisible = task.feedbackLink != null
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  companion object {
    @NonNls
    const val ACTION_ID: String = "Educational.LeaveFeedbackAction"
  }
}