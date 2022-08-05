package com.jetbrains.edu.learning.submissions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader
import com.jetbrains.edu.learning.taskDescription.ui.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TaskDescriptionBundle
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.SwingTextPanel
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabTextPanel
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.SUBMISSIONS_TAB
import com.jetbrains.edu.learning.ui.EduColors
import java.net.URL
import java.text.DateFormat
import java.util.*
import kotlin.math.roundToInt

class SubmissionsTab(project: Project) : AdditionalTab(project, SUBMISSIONS_TAB) {
  override val plainText: Boolean = true

  init {
    init()
  }

  private val panel: SwingTextPanel
    get() = innerTextPanel as SwingTextPanel

  override fun update(task: Task) {
    if (!task.supportSubmissions) return

    val submissionsManager = SubmissionsManager.getInstance(project)
    val descriptionText = StringBuilder()
    var customLinkHandler: SwingToolWindowLinkHandler? = null

    if (submissionsManager.isLoggedIn()) {
      val submissionsList = submissionsManager.getSubmissionsFromMemory(setOf(task.id))
      if (submissionsList.isNullOrEmpty()) {
        descriptionText.addEmptySubmissionsMessage()
      }
      else {
        descriptionText.addSubmissions(submissionsList)
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

  override fun createTextPanel(): TabTextPanel = SwingTextPanel(project)

  fun showLoadingPanel(platformName: String) = panel.showLoadingSubmissionsPanel(platformName)

  companion object {
    private const val SUBMISSION_PROTOCOL = "submission://"
    private const val SUBMISSION_DIFF_URL = "${SUBMISSION_PROTOCOL}diff/"
    private const val SUBMISSION_LOGIN_URL = "${SUBMISSION_PROTOCOL}login/"

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
        val submission = submissionsManager.getSubmission(task, submissionId) ?: return true
        runInEdt {
          showDiff(project, task, submission)
        }
        return true
      }
    }

    private fun StringBuilder.addEmptySubmissionsMessage() {
      append("<a $textStyleHeader>${EduCoreBundle.message("submissions.empty")}")
    }

    private fun StringBuilder.addSubmissions(submissionsNext: List<Submission>) {
      append("<ul style=list-style-type:none;margin:0;padding:0;>")
      submissionsNext.forEach { submission ->
        append(submissionLink(submission))
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
          val submissionFileContent = DiffContentFactory.getInstance().create(
            StepikSolutionsLoader.removeAllTags(submissionText),
            virtualFile.fileType)
          SimpleDiffRequest(EduCoreBundle.message("submissions.compare"),
                            currentFileContent,
                            submissionFileContent,
                            EduCoreBundle.message("submissions.local"),
                            EduCoreBundle.message("submissions.submission"))
        }
      }
      DiffManager.getInstance().showDiff(project, SimpleDiffRequestChain(requests), DiffDialogHints.FRAME)
    }

    private fun submissionLink(submission: Submission): String? {
      val time = submission.time ?: return null
      val pictureSize = (StyleManager().bodyFontSize * 0.75).roundToInt()
      val text = formatDate(time)
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
      return (icon as IconLoader.CachedImageIcon).url
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
