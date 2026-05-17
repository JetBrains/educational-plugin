package com.jetbrains.edu.learning.submissions.ui

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.submissions.*
import com.jetbrains.edu.learning.submissions.ui.linkHandler.LoginLinkHandler
import com.jetbrains.edu.learning.submissions.ui.linkHandler.SubmissionsDifferenceLinkHandler
import com.jetbrains.edu.learning.submissions.ui.linkHandler.SubmissionsDifferenceLinkHandler.Companion.getSubmissionDiffLink
import com.jetbrains.edu.learning.taskToolWindow.links.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.SwingTextPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TaskToolWindowCardTextTab
import java.util.concurrent.CompletableFuture

/**
 * Constructor is called exclusively in [com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManager.createTab]
 * and MUST NOT be called in any other places
 */
open class SubmissionsTab(project: Project) : TaskToolWindowCardTextTab(project, SUBMISSIONS_TAB) {

  override val uiMode: JavaUILibrary
    get() = JavaUILibrary.SWING

  protected val panel: SwingTextPanel
    get() = cards().first() as SwingTextPanel

  override fun update(task: Task) {
    if (!task.supportSubmissions) return

    CompletableFuture.runAsync({
      val submissionsManager = SubmissionsManager.getInstance(project)
      val isLoggedIn = submissionsManager.isLoggedIn()

      updateContent(task, isLoggedIn)
    }, ProcessIOExecutorService.INSTANCE)
  }

  protected open fun updateContent(task: Task, isLoggedIn: Boolean) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    val (descriptionText, customLinkHandler) = prepareSubmissionsContent(submissionsManager, task, isLoggedIn)
    project.invokeLater {
      updatePanel(panel, descriptionText, customLinkHandler)
    }
  }

  protected fun updatePanel(panel: SwingTextPanel, text: String, linkHandler: SwingToolWindowLinkHandler?) = panel.apply {
    hideLoadingSubmissionsPanel()
    updateLinkHandler(linkHandler)
    setText(text)
  }

  @RequiresBackgroundThread
  protected fun prepareSubmissionsContent(submissionsManager: SubmissionsManager, task: Task, isLoggedIn: Boolean): Pair<String, SwingToolWindowLinkHandler?> {
    val submissionsList = submissionsManager.getSubmissionsFromMemory(setOf(task.id))

    if (!isLoggedIn) {
      if (task.course.isMarketplace && submissionsList.isNotEmpty()) {
        return getSubmissionsText(submissionsList) to SubmissionsDifferenceLinkHandler(project, task, submissionsManager)
      }
      else {
        return LoginLinkHandler.getLoginText() to LoginLinkHandler(project, submissionsManager)
      }
    }

    if (submissionsList.isEmpty()) {
      return emptySubmissionsMessage() to null
    }

    return getSubmissionsText(submissionsList) to
      SubmissionsDifferenceLinkHandler(project, task, submissionsManager)
  }

  fun showLoadingPanel(platformName: String) = panel.showLoadingSubmissionsPanel(platformName)

  protected fun getSubmissionsText(submissionsNext: List<Submission>): String =
    submissionsNext.joinToString(separator = "", prefix = OPEN_UL_TAG, postfix = CLOSE_UL_TAG, transform = ::submissionLink)

  companion object {
    const val SUBMISSION_PROTOCOL = "submission://"

    private const val OPEN_UL_TAG = "<ul style=list-style-type:none;margin:0;padding:0;>"
    private const val CLOSE_UL_TAG = "</ul>"

    val textStyleHeader: String
      get() = StyleManager().textStyleHeader

    private fun emptySubmissionsMessage(): String = "<a $textStyleHeader>${EduCoreBundle.message("submissions.empty")}"

    private fun submissionLink(submission: Submission): String {
      val time = submission.time ?: return ""
      val pictureSize = StyleManager().bodyLineHeight
      val date = formatDate(time)

      return "<li><h><img src=${getImageUrl(submission.status)} hspace=6 width=${pictureSize} height=${pictureSize}/></h>" +
             "<a $textStyleHeader;color:${getLinkColor(submission)} href=${getSubmissionDiffLink(submission.id)}> ${date}</a></li>"
    }
  }
}
