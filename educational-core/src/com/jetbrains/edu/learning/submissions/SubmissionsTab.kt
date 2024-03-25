package com.jetbrains.edu.learning.submissions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.ColorUtil
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.ApplyCodeAction.Companion.FILENAMES_KEY
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.actions.ShareMySolutionsAction
import com.jetbrains.edu.learning.marketplace.isMarketplaceCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isCommunitySolutionsAllowed
import com.jetbrains.edu.learning.taskToolWindow.ui.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.TaskToolWindowBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TaskToolWindowCardTextTab
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.SwingTextPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import com.jetbrains.edu.learning.ui.EduColors
import java.awt.BorderLayout
import java.net.URL
import java.text.DateFormat
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.swing.JButton

@Suppress("UnstableApiUsage")
class SubmissionsTab(project: Project) : TaskToolWindowCardTextTab(project, SUBMISSIONS_TAB) {

  override val uiMode: JavaUILibrary
    get() = JavaUILibrary.SWING

  private val panel: SwingTextPanel
    get() = cards().first() as SwingTextPanel

  private lateinit var communityPanel: SwingTextPanel

  private lateinit var segmentedButton: SegmentedButton<JButton>

  private val isCommunityTabAvailable: Boolean = Registry.`is`(ShareMySolutionsAction.REGISTRY_KEY, false) && project.isMarketplaceCourse()

  init {
    if (project.isMarketplaceCourse()) {
      communityPanel = cards().last() as SwingTextPanel
      communityPanel.isVisible = false
      addSegmentedButton()
    }
  }

  override fun update(task: Task) {
    if (!task.supportSubmissions) return

    CompletableFuture.runAsync({
      val submissionsManager = SubmissionsManager.getInstance(project)
      val isLoggedIn = submissionsManager.isLoggedIn()
      updateCommunityUI(isLoggedIn)

      project.invokeLater {
        updateSubmissionsContent(task, isLoggedIn)
      }
    }, ProcessIOExecutorService.INSTANCE)
  }

  private fun updateCommunityUI(isLoggedIn: Boolean) {
    if (isLoggedIn && isCommunityTabAvailable) {
      headerPanel.isVisible = true

      panel.border = JBUI.Borders.empty()
      communityPanel.border = JBUI.Borders.empty()
      panel.component.border = JBUI.Borders.emptyLeft(34)
      communityPanel.component.border = JBUI.Borders.emptyLeft(34)

      segmentedButton.selectedItem = MY
      segmentedButton.visible(true)
      communityPanel.isVisible = true
      panel.addAdjustableBorder()
      communityPanel.addAdjustableBorder()
    }
    else {
      headerPanel.isVisible = false

      panel.border = JBUI.Borders.empty(15, 15, 0, 0)
      panel.component.border = JBUI.Borders.empty()
      if (project.isMarketplaceCourse()) {
        segmentedButton.visible(false)
        communityPanel.isVisible = false
      }
    }
  }

  private fun updateSubmissionsContent(task: Task, isLoggedIn: Boolean) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    val (descriptionText, customLinkHandler) = prepareSubmissionsContent(submissionsManager, task, isLoggedIn)

    if (isCommunityTabAvailable) {
      val (communityDescriptionText, communityLinkHandler) = prepareCommunityContent(task, submissionsManager)
      communityPanel.apply {
        hideLoadingSubmissionsPanel()
        updateLinkHandler(communityLinkHandler)
        setText(communityDescriptionText)
      }
    }

    panel.apply {
      hideLoadingSubmissionsPanel()
      updateLinkHandler(customLinkHandler)
      setText(descriptionText)
    }
  }

  private fun prepareCommunityContent(task: Task, submissionsManager: SubmissionsManager): Pair<String, SwingToolWindowLinkHandler?> {
    if (task.isCommunitySolutionsAllowed()) {
      segmentedButton.enableCommunityButton()
      val submissionsList = submissionsManager.getCommunitySubmissionsFromMemory(task.id)

      if (submissionsList.isNullOrEmpty()) {
        return EMPTY_COMMUNITY_SOLUTIONS_MESSAGE to null
      }

      return getSubmissionsText(submissionsList) to SubmissionsDifferenceLinkHandler(project, task, submissionsManager, true)
    }
    else {
      segmentedButton.disableCommunityButton()
      return EduCoreBundle.message("submissions.button.community.tooltip.text.disabled") to null
    }
  }

  private fun prepareSubmissionsContent(submissionsManager: SubmissionsManager, task: Task, isLoggedIn: Boolean): Pair<String, SwingToolWindowLinkHandler?> {
    val submissionsList = submissionsManager.getSubmissionsFromMemory(setOf(task.id))

    if (!isLoggedIn && task.course.isMarketplace && submissionsList?.isNotEmpty() == true) {
      return getSubmissionsText(submissionsList) to SubmissionsDifferenceLinkHandler(project, task, submissionsManager)
    }

    if (!isLoggedIn) {
      return getLoginText(submissionsManager) to LoginLinkHandler(project, submissionsManager)
    }

    if (submissionsList.isNullOrEmpty()) {
      return EMPTY_SUBMISSIONS_MESSAGE to null
    }

    return getSubmissionsText(submissionsList, isToShowSubmissionsIds(task)) to SubmissionsDifferenceLinkHandler(project, task, submissionsManager)
  }

  fun showLoadingPanel(platformName: String) = panel.showLoadingSubmissionsPanel(platformName)

  fun showLoadingCommunityPanel(platformName: String) {
    if (!isCommunityTabAvailable) return
    communityPanel.showLoadingSubmissionsPanel(platformName)
  }

  private fun addSegmentedButton() = headerPanel.add(panel {
    row {
      segmentedButton = segmentedButton(SEGMENTED_BUTTON_ITEMS) { segmentedButtonRenderer(it) }.apply {
        selectedItem = MY
        visible(false)
        whenItemSelected {
          when (it) {
            MY -> showFirstCard()
            COMMUNITY -> showLastCard()
          }
        }
      }
    }
  }, BorderLayout.CENTER)

  companion object {
    private const val SUBMISSION_PROTOCOL = "submission://"
    private const val SUBMISSION_DIFF_URL = "${SUBMISSION_PROTOCOL}diff/"
    private const val SUBMISSION_LOGIN_URL = "${SUBMISSION_PROTOCOL}login/"
    private const val OPEN_UL_TAG = "<ul style=list-style-type:none;margin:0;padding:0;>"
    private const val CLOSE_UL_TAG = "</ul>"
    private val MY = JButton(EduCoreBundle.message("submissions.button.my"))
    private val COMMUNITY = JButton(EduCoreBundle.message("submissions.button.community")).apply { isEnabled = false }
    private val SEGMENTED_BUTTON_ITEMS = listOf(MY, COMMUNITY)
    private val EMPTY_SUBMISSIONS_MESSAGE = "<a $textStyleHeader>${EduCoreBundle.message("submissions.empty")}"
    private val EMPTY_COMMUNITY_SOLUTIONS_MESSAGE = "<a $textStyleHeader>${EduCoreBundle.message("submissions.community.empty")}"
    const val OPEN_PLACEHOLDER_TAG = "<placeholder>"
    const val CLOSE_PLACEHOLDER_TAG = "</placeholder>"

    private val textStyleHeader: String
      get() = StyleManager().textStyleHeader

    private class LoginLinkHandler(
      project: Project, private val submissionsManager: SubmissionsManager
    ) : SwingToolWindowLinkHandler(project) {
      override fun process(url: String, referUrl: String?): Boolean {
        if (!url.startsWith(SUBMISSION_LOGIN_URL)) return false

        submissionsManager.doAuthorize()
        return true
      }
    }

    private class SubmissionsDifferenceLinkHandler(
      project: Project, private val task: Task, private val submissionsManager: SubmissionsManager, private val isCommunity: Boolean = false
    ) : SwingToolWindowLinkHandler(project) {
      override fun process(url: String, referUrl: String?): Boolean {
        if (!url.startsWith(SUBMISSION_DIFF_URL)) return false

        val submissionId = url.substringAfter(SUBMISSION_DIFF_URL).toInt()
        ApplicationManager.getApplication().executeOnPooledThread {
          val submission = submissionsManager.getSubmissionWithSolutionText(task, submissionId) ?: return@executeOnPooledThread
          runInEdt {
            showDiff(project, task, submission, isCommunity)
          }
        }
        return true
      }
    }

    /**
     * Showing submissions ids is needed for `ApplyHyperskillSubmission` action testing
     */
    private fun isToShowSubmissionsIds(task: Task) = task.course is HyperskillCourse && isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)

    private fun getSubmissionsText(submissionsNext: List<Submission>, isToShowSubmissionsIds: Boolean = false): String = submissionsNext.map {
      submissionLink(it, isToShowSubmissionsIds)
    }.joinTo(
      StringBuilder(OPEN_UL_TAG), separator = ""
    ).append(CLOSE_UL_TAG).toString()

    private fun getLoginText(submissionsManager: SubmissionsManager): String = if (!RemoteEnvHelper.isRemoteDevServer()) {
      "<a $textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=$SUBMISSION_LOGIN_URL>" +
      EduCoreBundle.message("submissions.login", submissionsManager.getPlatformName()) + "</a>"
    }
    else {
      EduCoreBundle.message("submissions.wait.user.data.being.retrieved")
    }

    private fun showDiff(project: Project, task: Task, submission: Submission, isCommunity: Boolean) {
      val taskFiles = task.taskFiles.values.toMutableList()
      val submissionTexts = submission.getSubmissionTexts(task.name) ?: return
      val submissionTaskFiles = taskFiles.filter { it.isVisible && !it.isTestFile }
      val submissionTaskFilePaths = mutableListOf<String>()
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
          submissionTaskFilePaths.add(virtualFile.path)
          val submissionFileContent = DiffContentFactory.getInstance().create(submissionText.removeAllTags(), virtualFile.fileType)
          createSimpleDiffRequest(currentFileContent, submissionFileContent, submission, isCommunity)
        }
      }
      val diffRequestChain = SimpleDiffRequestChain(requests)
      diffRequestChain.putUserData(FILENAMES_KEY, submissionTaskFilePaths)
      DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
    }

    private fun createSimpleDiffRequest(currentContent: DocumentContent, submissionContent: DocumentContent, submission: Submission, isCommunity: Boolean): SimpleDiffRequest {
      val (title, title2) = if (!isCommunity) {
        EduCoreBundle.message("submissions.compare") to EduCoreBundle.message("submissions.submission")
      }
      else {
        val time = submission.time
        val formattedDate = time?.let { formatDate(time) } ?: ""
        EduCoreBundle.message("submissions.compare.community", formattedDate) to EduCoreBundle.message("submissions.community")
      }

      return SimpleDiffRequest(title, currentContent, submissionContent, EduCoreBundle.message("submissions.local"), title2)
    }

    private fun String.removeAllTags(): String = replace(OPEN_PLACEHOLDER_TAG.toRegex(), "").replace(CLOSE_PLACEHOLDER_TAG.toRegex(), "")

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
        TaskToolWindowBundle.value("correct.label.foreground.high.contrast")
      }
      else {
        "#${ColorUtil.toHex(EduColors.correctLabelForeground)}"
      }
    }

    private fun getWrongLinkColor(): String {
      return if (StyleResourcesManager.isHighContrast()) {
        TaskToolWindowBundle.value("wrong.label.foreground.high.contrast")
      }
      else {
        "#${ColorUtil.toHex(EduColors.wrongLabelForeground)}"
      }
    }
  }
}
