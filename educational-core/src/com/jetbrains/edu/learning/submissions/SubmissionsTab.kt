package com.jetbrains.edu.learning.submissions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TaskDescriptionBundle
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.SwingTextPanel
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.SUBMISSIONS_TAB
import com.jetbrains.edu.learning.ui.EduColors
import java.net.URL
import java.text.DateFormat
import java.util.*
import java.util.concurrent.CompletableFuture

class SubmissionsTab(project: Project) : AdditionalTab(project, SUBMISSIONS_TAB) {
  override val uiMode: JavaUILibrary
    get() = JavaUILibrary.SWING

  init {
    init()
  }

  private val panel: SwingTextPanel
    get() = innerTextPanel as SwingTextPanel

  override fun update(task: Task) {
    if (!task.supportSubmissions) return

    CompletableFuture.runAsync({
      val submissionsManager = SubmissionsManager.getInstance(project)
      val isLoggedIn = submissionsManager.isLoggedIn()
      project.invokeLater {
        updateSubmissionsContent(task, isLoggedIn)
      }
    }, ProcessIOExecutorService.INSTANCE)
  }

  private fun updateSubmissionsContent(task: Task, isLoggedIn: Boolean) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    val descriptionText = StringBuilder()
    var customLinkHandler: SwingToolWindowLinkHandler? = null

    if (isLoggedIn) {
      val submissionsList = submissionsManager.getSubmissionsFromMemory(setOf(task.id))
      if (submissionsList.isEmpty()) {
        descriptionText.addEmptySubmissionsMessage()
      }
      else {
        // we need to show submissions ids for `ApplyHyperskillSubmission` action testing
        val course = task.course
        val isToShowSubmissionsIds = course is HyperskillCourse && isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)

        descriptionText.addSubmissions(submissionsList, isToShowSubmissionsIds)
        customLinkHandler = SubmissionsDifferenceLinkHandler(project, task, submissionsManager)
      }
    }
    else {
      descriptionText.addLoginText(submissionsManager)
      customLinkHandler = LoginLinkHandler(project, submissionsManager)
    }

    panel.apply {
      hideLoadingSubmissionsPanel()
      updateLinkHandler(customLinkHandler)
    }
    setText(descriptionText.toString())
  }

  fun showLoadingPanel(platformName: String) = panel.showLoadingSubmissionsPanel(platformName)

  companion object {
    private const val SUBMISSION_PROTOCOL = "submission://"
    private const val SUBMISSION_DIFF_URL = "${SUBMISSION_PROTOCOL}diff/"
    private const val SUBMISSION_LOGIN_URL = "${SUBMISSION_PROTOCOL}login/"
    const val OPEN_PLACEHOLDER_TAG = "<placeholder>"
    const val CLOSE_PLACEHOLDER_TAG = "</placeholder>"

    private val textStyleHeader: String
      get() = StyleManager().textStyleHeader

    private class LoginLinkHandler(
      project: Project,
      private val submissionsManager: SubmissionsManager
    ) : SwingToolWindowLinkHandler(project) {
      override fun process(url: String, referUrl: String?): Boolean {
        if (!url.startsWith(SUBMISSION_LOGIN_URL)) return false

        submissionsManager.doAuthorize()
        return true
      }
    }

    private class SubmissionsDifferenceLinkHandler(
      project: Project,
      private val task: Task,
      private val submissionsManager: SubmissionsManager
    ) : SwingToolWindowLinkHandler(project) {
      override fun process(url: String, referUrl: String?): Boolean {
        if (!url.startsWith(SUBMISSION_DIFF_URL)) return false

        val submissionId = url.substringAfter(SUBMISSION_DIFF_URL).toInt()
        ApplicationManager.getApplication().executeOnPooledThread {
          val submission = submissionsManager.getSubmissionWithSolutionText(task, submissionId) ?: return@executeOnPooledThread
          runInEdt {
            showDiff(project, task, submission)
          }
        }
        return true
      }
    }

    private fun StringBuilder.addEmptySubmissionsMessage() {
      append("<a $textStyleHeader>${EduCoreBundle.message("submissions.empty")}")
    }

    private fun StringBuilder.addSubmissions(submissionsNext: List<Submission>, isToShowSubmissionsIds: Boolean) {
      append("<ul style=list-style-type:none;margin:0;padding:0;>")
      submissionsNext.forEach { submission ->
        append(submissionLink(submission, isToShowSubmissionsIds))
      }
      append("</ul>")
    }

    private fun StringBuilder.addLoginText(submissionsManager: SubmissionsManager) {
      append("<a $textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=$SUBMISSION_LOGIN_URL>" +
             EduCoreBundle.message("submissions.login", submissionsManager.getPlatformName()) + "</a>")
    }

    private fun showDiff(project: Project, task: Task, submission: Submission) {
      val taskFiles = task.taskFiles.values.toMutableList()
      val submissionTexts = submission.getSubmissionTexts(task.name) ?: return
      val submissionTaskFiles = taskFiles.filter { it.isVisible && !it.isTestFile }
      val requests = submissionTaskFiles.mapNotNull {
        val virtualFile = it.getVirtualFile(project) ?: error("VirtualFile for ${it.name} not found")
        val documentText = FileDocumentManager.getInstance().getDocument(virtualFile)?.text
        val currentFileContent = if (documentText != null) DiffContentFactory.getInstance().create(documentText, virtualFile.fileType)
        else null
        val submissionText = submissionTexts[it.name] ?: submissionTexts[task.name]
        if (submissionText == null || currentFileContent == null) {
          null
        }
        else {
          val submissionFileContent = DiffContentFactory.getInstance().create(submissionText.removeAllTags(), virtualFile.fileType)
          SimpleDiffRequest(EduCoreBundle.message("submissions.compare"),
                            currentFileContent,
                            submissionFileContent,
                            EduCoreBundle.message("submissions.local"),
                            EduCoreBundle.message("submissions.submission"))
        }
      }
      DiffManager.getInstance().showDiff(project, SimpleDiffRequestChain(requests), DiffDialogHints.FRAME)
    }

    private fun String.removeAllTags(): String =
      replace(OPEN_PLACEHOLDER_TAG.toRegex(), "").replace(CLOSE_PLACEHOLDER_TAG.toRegex(), "")

    private fun submissionLink(submission: Submission, isToShowSubmissionsIds: Boolean): String? {
      val time = submission.time ?: return null
      val pictureSize = StyleManager().bodyLineHeight
      val date = formatDate(time)
      val text = if (isToShowSubmissionsIds) {
        "$date submission.id = ${submission.id}"
      }
      else {
        date
      }

      return "<li><h><img src=${getImageUrl(submission.status)} hspace=6 width=${pictureSize} height=${pictureSize}/></h>" +
             "<a $textStyleHeader;color:${getLinkColor(submission)} href=$SUBMISSION_DIFF_URL${submission.id}> ${text}</a></li>"
    }

    private fun formatDate(time: Date): String {
      val calendar = GregorianCalendar()
      calendar.time = time
      val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault())
      return formatter.format(calendar.time)
    }

    private fun getImageUrl(status: String?): URL? {
      val icon = when (status) {
        CORRECT -> if (StyleResourcesManager.isHighContrast()) EducationalCoreIcons.TaskSolvedNoFrameHighContrast else EducationalCoreIcons.TaskSolvedNoFrame
        else -> if (StyleResourcesManager.isHighContrast()) EducationalCoreIcons.TaskFailedNoFrameHighContrast else EducationalCoreIcons.TaskFailedNoFrame
      }
      @Suppress("UnstableApiUsage")
      return (icon as CachedIcon).url
    }

    private fun getLinkColor(submission: Submission): String {
      return when (submission.status) {
        CORRECT -> getCorrectLinkColor()
        else -> getWrongLinkColor()
      }
    }

    private fun getCorrectLinkColor(): String {
      return if (StyleResourcesManager.isHighContrast()) {
        TaskDescriptionBundle.value("correct.label.foreground.high.contrast")
      }
      else {
        "#${ColorUtil.toHex(EduColors.correctLabelForeground)}"
      }
    }

    private fun getWrongLinkColor(): String {
      return if (StyleResourcesManager.isHighContrast()) {
        TaskDescriptionBundle.value("wrong.label.foreground.high.contrast")
      }
      else {
        "#${ColorUtil.toHex(EduColors.wrongLabelForeground)}"
      }
    }
  }
}
