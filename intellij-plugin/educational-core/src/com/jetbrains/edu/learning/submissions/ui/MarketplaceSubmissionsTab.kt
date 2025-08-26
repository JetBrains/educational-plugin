package com.jetbrains.edu.learning.submissions.ui

import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.AncestorListenerAdapter
import com.intellij.ui.GotItTooltip
import com.intellij.ui.dsl.builder.SegmentedButton
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.canShowCommunitySolutions
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.invokeLater
import com.jetbrains.edu.learning.marketplace.areCommunitySolutionsSupported
import com.jetbrains.edu.learning.marketplace.isMarketplaceCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.submissions.Submission
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.submissions.ui.linkHandler.SubmissionsDifferenceLinkHandler
import com.jetbrains.edu.learning.submissions.ui.linkHandler.SubmissionsDifferenceLinkHandler.Companion.showMoreLink
import com.jetbrains.edu.learning.submissions.ui.segmentedButton.CommunitySegmentedButtonItem
import com.jetbrains.edu.learning.submissions.ui.segmentedButton.MySegmentedButtonItem
import com.jetbrains.edu.learning.submissions.ui.segmentedButton.SegmentedButtonItem
import com.jetbrains.edu.learning.taskToolWindow.links.SwingToolWindowLinkHandler
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.SwingTextPanel
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.event.AncestorEvent

/**
 * Constructor is called exclusively in [com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabManager.createTab]
 * and MUST NOT be called in any other places
 */
@Suppress("UnstableApiUsage")
class MarketplaceSubmissionsTab(project: Project) : SubmissionsTab(project) {

  private var communityPanel: SwingTextPanel

  private lateinit var segmentedButton: SegmentedButton<SegmentedButtonItem>

  init {
    if (!project.isMarketplaceCourse()) error("Can't create this Tab for non-Marketplace course")

    communityPanel = cards().last() as SwingTextPanel
    createCommunityUI()
  }

  override fun updateContent(task: Task, isLoggedIn: Boolean) {
    val submissionsManager = SubmissionsManager.getInstance(project)

    val isAllowedToLoadCommunitySolutions = task.canShowCommunitySolutions()
    val areCommunitySolutionsSupported = (task.course as? EduCourse)?.areCommunitySolutionsSupported() ?: false

    val (descriptionText, customLinkHandler) = prepareSubmissionsContent(submissionsManager, task, isLoggedIn)
    val (communityDescriptionText, communityLinkHandler) = prepareCommunityContent(
      task,
      submissionsManager,
      isAllowedToLoadCommunitySolutions
    )

    val isCommunityButtonEnabled = isLoggedIn && isAllowedToLoadCommunitySolutions
    val toolTipText = when {
      !isLoggedIn -> EduCoreBundle.message("submissions.button.community.tooltip.text.login")
      isCommunityButtonEnabled -> EduCoreBundle.message("submissions.button.community.tooltip.text.enabled")
      !areCommunitySolutionsSupported -> EduCoreBundle.message("submissions.button.community.tooltip.text.not.supported")
      else -> EduCoreBundle.message("submissions.button.community.tooltip.text.disabled")
    }

    project.invokeLater {
      segmentedButton.updateCommunityButton(
        isEnabled = isCommunityButtonEnabled,
        tooltipText = toolTipText,
      )
      updatePanel(panel, descriptionText, customLinkHandler)
      updatePanel(communityPanel, communityDescriptionText, communityLinkHandler)
    }
  }

  private fun prepareCommunityContent(
    task: Task, submissionsManager: SubmissionsManager, isAllowedToShowCommunitySolutions: Boolean
  ): Pair<String, SwingToolWindowLinkHandler?> {
    if (isAllowedToShowCommunitySolutions) {
      val submissionsList = submissionsManager.getCommunitySubmissionsFromMemory(task.id)

      if (submissionsList.isNullOrEmpty()) {
        return emptyCommunitySolutionsMessage() to null
      }
      val isToDisplayShowMore = submissionsManager.hasMoreCommunitySubmissions(task.id)
      return getCommunitySolutionsText(submissionsList, isToDisplayShowMore).toString() to SubmissionsDifferenceLinkHandler(
        project,
        task,
        submissionsManager,
        isCommunity = true
      )
    }
    else {
      return EduCoreBundle.message("submissions.button.community.tooltip.text.disabled") to null
    }
  }

  private fun SegmentedButton<SegmentedButtonItem>.updateCommunityButton(
    isEnabled: Boolean,
    tooltipText: String,
  ) {
    val communityButton = items.first { it is CommunitySegmentedButtonItem }
    communityButton.isEnabled = isEnabled
    communityButton.toolTipText = tooltipText

    if (!isEnabled) {
      selectedItem = items.first()
    }

    update(communityButton)
  }

  private fun createCommunityUI() {
    val segmentedButtonItems = listOf(MySegmentedButtonItem(), CommunitySegmentedButtonItem().apply { isEnabled = false })
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

  private fun createSegmentedButton(segmentedButtonItems: List<SegmentedButtonItem>): DialogPanel = panel {
    row {
      segmentedButton = segmentedButton(segmentedButtonItems) {
        text = it.text
        enabled = it.isEnabled
        toolTipText = it.toolTipText
      }.apply {
        selectedItem = segmentedButtonItems.first { it is MySegmentedButtonItem }
        whenItemSelected {
          when (it) {
            is MySegmentedButtonItem -> showFirstCard()
            is CommunitySegmentedButtonItem -> {
              showLastCard()
              EduCounterUsageCollector.openCommunityTab()
            }
          }
        }
      }
    }
  }

  private fun JComponent.addGotItTooltip(segmentedButtonItem: SegmentedButtonItem) =
    addAncestorListener(object : AncestorListenerAdapter() {
      override fun ancestorAdded(e: AncestorEvent) {
        val gotItTooltip = GotItTooltip(
          GOT_IT_ID, EduCoreBundle.message("submissions.button.community.tooltip.text"), this@MarketplaceSubmissionsTab
        ).withHeader(EduCoreBundle.message("submissions.button.community.tooltip.header"))

        val communityJComponent = UIUtil.uiTraverser(this@addGotItTooltip).filter(ActionButton::class.java).filter {
          it.action.templateText == segmentedButtonItem.text
        }.first() as JComponent

        if (communityJComponent.isEnabled) {
          gotItTooltip.show(communityJComponent, GotItTooltip.BOTTOM_MIDDLE)
        }
      }
    })

  fun isCommunityTabShowing(): Boolean {
    return segmentedButton.selectedItem is CommunitySegmentedButtonItem
  }

  fun showMyTab() {
    segmentedButton.selectedItem = segmentedButton.items.first { it is MySegmentedButtonItem }
  }

  fun showCommunityTab() {
    segmentedButton.selectedItem = segmentedButton.items.first { it is CommunitySegmentedButtonItem }
  }

  fun showLoadingCommunityPanel(platformName: String) {
    communityPanel.showLoadingSubmissionsPanel(platformName)
  }

  private fun getCommunitySolutionsText(communitySolutions: List<Submission>, showMore: Boolean): StringBuilder {
    val submissionsStringBuilder = getSubmissionsText(communitySolutions)
    if (showMore) {
      submissionsStringBuilder.append(showMoreLink())
    }
    return submissionsStringBuilder
  }

  companion object {
    @NonNls
    private const val GOT_IT_ID: String = "submissions.tab.community.button"

    private fun emptyCommunitySolutionsMessage(): String = "<a $textStyleHeader>${EduCoreBundle.message("submissions.community.empty")}"
  }
}