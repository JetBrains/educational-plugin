package com.jetbrains.edu.learning.submissions

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.ui.AncestorListenerAdapter
import com.intellij.ui.ColorUtil
import com.intellij.ui.GotItTooltip
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.icons.CachedImageIcon
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.actions.ApplyCodeAction
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CORRECT
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.isTestFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.marketplace.actions.ReportCommunitySolutionAction
import com.jetbrains.edu.learning.marketplace.isMarketplaceCourse
import com.jetbrains.edu.learning.marketplace.userAgreement.UserAgreementDialog
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.taskToolWindow.links.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.TaskToolWindowBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.SwingTextPanel
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TaskToolWindowCardTextTab
import com.jetbrains.edu.learning.ui.EduColors
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.net.URL
import java.text.DateFormat
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent
import javax.swing.event.AncestorEvent

/**
 * Constructor is called exclusively in [com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManager.createTab]
 * and MUST NOT be called in any other places
 */
class SubmissionsTab(project: Project) : TaskToolWindowCardTextTab(project, SUBMISSIONS_TAB) {

  override val uiMode: JavaUILibrary
    get() = JavaUILibrary.SWING

  private val panel: SwingTextPanel
    get() = cards().first() as SwingTextPanel

  private lateinit var communityPanel: SwingTextPanel

  private lateinit var segmentedButton: SegmentedButton<SegmentedButtonItem>

  private val isMarketplaceCourse: Boolean = project.isMarketplaceCourse()

  init {
    if (isMarketplaceCourse) {
      initCommunityUI()
    }
  }

  override fun update(task: Task) {
    if (!task.supportSubmissions) return

    CompletableFuture.runAsync({
      val submissionsManager = SubmissionsManager.getInstance(project)
      val isLoggedIn = submissionsManager.isLoggedIn()

      updateSubmissionsContent(task, isLoggedIn)
    }, ProcessIOExecutorService.INSTANCE)
  }

  private fun initCommunityUI() {
    communityPanel = cards().last() as SwingTextPanel

    val segmentedButtonItems = listOf(MySegmentedButton(), CommunitySegmentedButton().apply { isEnabled = false })
    val segmentedButtonPanel = createSegmentedButton(segmentedButtonItems)
    segmentedButtonPanel.addGotItTooltip(segmentedButtonItems.last())
    headerPanel.apply {
      add(segmentedButtonPanel, BorderLayout.CENTER)
      isVisible = true
    }

    val emptyBorder = JBUI.Borders.empty()
    val emptyLeftBorder = JBUI.Borders.emptyLeft(33)
    panel.apply {
      border = emptyBorder
      component.border = emptyLeftBorder
      addAdjustableBorder()
    }
    communityPanel.apply {
      border = emptyBorder
      component.border = emptyLeftBorder
      addAdjustableBorder()
    }
  }

  private fun updateSubmissionsContent(task: Task, isLoggedIn: Boolean) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    val isSolutionSharingAllowed = submissionsManager.isSolutionSharingAllowed()
    val isAllowedToLoadCommunitySolutions = submissionsManager.isAllowedToLoadCommunitySolutions(task)
    val (descriptionText, customLinkHandler) =
      prepareSubmissionsContent(submissionsManager, task, isLoggedIn)

    if (isMarketplaceCourse) {
      val (communityDescriptionText, communityLinkHandler) =
        prepareCommunityContent(task, submissionsManager, isAllowedToLoadCommunitySolutions, isSolutionSharingAllowed)

      project.invokeLater {
        segmentedButton.updateCommunityButton(
          isLoggedIn = isLoggedIn,
          isEnabled = isAllowedToLoadCommunitySolutions || !isSolutionSharingAllowed,
          isAgreementTooltip = !isSolutionSharingAllowed
        )
        updatePanel(panel, descriptionText, customLinkHandler)
        updatePanel(communityPanel, communityDescriptionText, communityLinkHandler)
      }
    }
    else {
      project.invokeLater {
        updatePanel(panel, descriptionText, customLinkHandler)
      }
    }
  }

  private fun updatePanel(panel: SwingTextPanel, text: String, linkHandler: SwingToolWindowLinkHandler?) = panel.apply {
    hideLoadingSubmissionsPanel()
    updateLinkHandler(linkHandler)
    setText(text)
  }

  private fun prepareCommunityContent(
    task: Task,
    submissionsManager: SubmissionsManager,
    isAllowedToShowCommunitySolutions: Boolean,
    isSolutionSharingAllowed: Boolean
  ): Pair<String, SwingToolWindowLinkHandler?> {
    if (isAllowedToShowCommunitySolutions && isSolutionSharingAllowed) {
      val submissionsList = submissionsManager.getCommunitySubmissionsFromMemory(task.id)

      if (submissionsList.isNullOrEmpty()) {
        return emptyCommunitySolutionsMessage() to null
      }

      return getSubmissionsText(submissionsList) to SubmissionsDifferenceLinkHandler(project, task, submissionsManager, true)
    }
    else if (!isSolutionSharingAllowed) {
      return getSolutionSharingAgreementPromptText() to LoginLinkHandler(project, submissionsManager)
    }
    else {
      return EduCoreBundle.message("submissions.button.community.tooltip.text.disabled") to null
    }
  }

  @RequiresBackgroundThread
  private fun prepareSubmissionsContent(submissionsManager: SubmissionsManager, task: Task, isLoggedIn: Boolean): Pair<String, SwingToolWindowLinkHandler?> {
    val submissionsList = submissionsManager.getSubmissionsFromMemory(setOf(task.id))

    if (!isLoggedIn) {
      if (task.course.isMarketplace && submissionsList.isNotEmpty()) {
        return getSubmissionsText(submissionsList) to SubmissionsDifferenceLinkHandler(project, task, submissionsManager)
      }
      else {
        return getLoginText() to LoginLinkHandler(project, submissionsManager)
      }
    }

    if (!submissionsManager.isSubmissionDownloadAllowed()) {
      return getAgreementPromptText() to LoginLinkHandler(project, submissionsManager)
    }

    if (submissionsList.isEmpty()) {
      return emptySubmissionsMessage() to null
    }

    return getSubmissionsText(submissionsList, isToShowSubmissionsIds(task)) to SubmissionsDifferenceLinkHandler(project, task, submissionsManager)
  }

  fun showLoadingPanel(platformName: String) = panel.showLoadingSubmissionsPanel(platformName)

  fun showLoadingCommunityPanel(platformName: String) {
    if (!isMarketplaceCourse) return
    communityPanel.showLoadingSubmissionsPanel(platformName)
  }

  fun showCommunityTab() {
    segmentedButton.selectedItem = segmentedButton.items.find { it is CommunitySegmentedButton }
  }

  fun showMyTab() {
    segmentedButton.selectedItem = segmentedButton.items.find { it is MySegmentedButton }
  }

  private fun createSegmentedButton(segmentedButtonItems: List<SegmentedButtonItem>) = panel {
    row {
      segmentedButton = segmentedButton(segmentedButtonItems) {
        text = it.text
        enabled = it.isEnabled
        toolTipText = it.toolTipText
      }.apply {
        selectedItem = segmentedButtonItems.first { it is MySegmentedButton }
        whenItemSelected {
          when (it) {
            is MySegmentedButton -> showFirstCard()
            is CommunitySegmentedButton -> {
              showLastCard()
              EduCounterUsageCollector.openCommunityTab()
            }
          }
        }
      }
    }
  }

  private fun SegmentedButton<SegmentedButtonItem>.updateCommunityButton(isLoggedIn: Boolean, isEnabled: Boolean, isAgreementTooltip: Boolean = false) {
    val communityButton = items.first { it is CommunitySegmentedButton }
    communityButton.isEnabled = isLoggedIn && isEnabled
    communityButton.toolTipText = when {
      !isLoggedIn -> EduCoreBundle.message("submissions.button.community.tooltip.text.login")
      isAgreementTooltip -> EduCoreBundle.message("submissions.tab.solution.sharing.agreement")
      isEnabled -> EduCoreBundle.message("submissions.button.community.tooltip.text.enabled")
      else -> EduCoreBundle.message("submissions.button.community.tooltip.text.disabled")
    }

    if (!isEnabled) {
      selectedItem = items.first()
    }

    update(communityButton)
  }

  private fun JComponent.addGotItTooltip(segmentedButtonItem: SegmentedButtonItem) =
    addAncestorListener(object : AncestorListenerAdapter() {
      override fun ancestorAdded(e: AncestorEvent) {
        val gotItTooltip = GotItTooltip(
          GOT_IT_ID, EduCoreBundle.message("submissions.button.community.tooltip.text"), this@SubmissionsTab
        ).withHeader(EduCoreBundle.message("submissions.button.community.tooltip.header"))

        val communityJComponent = UIUtil.uiTraverser(this@addGotItTooltip).filter(ActionButton::class.java).filter {
          it.action.templateText == segmentedButtonItem.text
        }.first() as JComponent

        if (communityJComponent.isEnabled) {
          gotItTooltip.show(communityJComponent, GotItTooltip.BOTTOM_MIDDLE)
        }
      }
    })

  companion object {
    private const val SUBMISSION_PROTOCOL = "submission://"
    private const val SUBMISSION_DIFF_URL = "${SUBMISSION_PROTOCOL}diff/"
    private const val SUBMISSION_LOGIN_URL = "${SUBMISSION_PROTOCOL}login/"
    private const val SUBMISSION_USER_AGREEMENT = "${SUBMISSION_PROTOCOL}agreement/"
    private const val OPEN_UL_TAG = "<ul style=list-style-type:none;margin:0;padding:0;>"
    private const val CLOSE_UL_TAG = "</ul>"
    const val OPEN_PLACEHOLDER_TAG = "<placeholder>"
    const val CLOSE_PLACEHOLDER_TAG = "</placeholder>"

    @NonNls
    private const val GOT_IT_ID: String = "submissions.tab.community.button"

    private val textStyleHeader: String
      get() = StyleManager().textStyleHeader

    private fun emptySubmissionsMessage(): String = "<a $textStyleHeader>${EduCoreBundle.message("submissions.empty")}"

    private fun emptyCommunitySolutionsMessage(): String = "<a $textStyleHeader>${EduCoreBundle.message("submissions.community.empty")}"

    private class LoginLinkHandler(
      project: Project, private val submissionsManager: SubmissionsManager
    ) : SwingToolWindowLinkHandler(project) {
      override fun process(url: String, referUrl: String?): Boolean {

        return when {
          url.startsWith(SUBMISSION_LOGIN_URL) -> {
            submissionsManager.doAuthorize()
            true
          }
          url.startsWith(SUBMISSION_USER_AGREEMENT) -> {
            runInEdt { UserAgreementDialog.showUserAgreementDialog(project) }
            true
          }
          else -> false
        }
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

    private fun getLoginText(): String = if (!RemoteEnvHelper.isRemoteDevServer()) {
      "<a $textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=$SUBMISSION_LOGIN_URL>" +
      EduCoreBundle.message("submissions.tab.login") + "</a>"
    }
    else {
      EduCoreBundle.message("submissions.wait.user.data.being.retrieved")
    }

    private fun getAgreementPromptText(): String =
      "<a $textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=$SUBMISSION_USER_AGREEMENT>" +
      EduCoreBundle.message("submissions.tab.agreement") + "</a>"

    private fun getSolutionSharingAgreementPromptText(): String =
      "<a $textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)} href=$SUBMISSION_USER_AGREEMENT>" +
      EduCoreBundle.message("submissions.tab.solution.sharing.agreement") + "</a>"

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
      diffRequestChain.putUserData(ApplyCodeAction.VIRTUAL_FILE_PATH_LIST, submissionTaskFilePaths)
      if (project.isMarketplaceCourse() && isCommunity) {
        diffRequestChain.putCommunitySolution(task, submission)
        EduCounterUsageCollector.communitySolutionDiffOpened()
      }
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

    private fun SimpleDiffRequestChain.putCommunitySolution(task: Task, submission: Submission) {
      putUserData(ReportCommunitySolutionAction.TASK_ID_KEY, task.id)
      putUserData(ReportCommunitySolutionAction.SUBMISSION_ID_KEY, submission.id)
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

      return (icon as CachedImageIcon).url
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
