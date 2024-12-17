package com.jetbrains.edu.aiHints.core.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.ui.NotificationBalloonRoundShadowBorderProvider
import com.intellij.ui.RoundedLineBorder
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.ui.EduColors
import org.jetbrains.annotations.Nls
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.border.CompoundBorder

open class HintInlineBanner(
  private val project: Project,
  @Nls message: String,
  status: Status = Status.Success
) : InlineBanner(message, status.toNotificationBannerStatus()) {
  init {
    setIcon(EduAiHintsIcons.Hint)
    isOpaque = false

    status.toolTipText?.let {
      toolTipText = it
    }
    border = createBorder(status.borderColor)
    background = status.backgroundColor
  }

  @RequiresEdt
  fun display() {
    TaskToolWindowView.getInstance(project).addInlineBannerToCheckPanel(this@HintInlineBanner)
  }

  enum class Status(val backgroundColor: Color, val borderColor: Color, val toolTipText: String? = null) {
    Success(EduColors.aiGetHintInlineBannersBackgroundColor, EduColors.aiGetHintInlineBannersBorderColor, EduAIHintsCoreBundle.message("hints.label.ai.generated.content.tooltip")),
    Error(JBUI.CurrentTheme.Banner.ERROR_BACKGROUND, JBUI.CurrentTheme.Banner.ERROR_BORDER_COLOR, null);

    fun toNotificationBannerStatus(): EditorNotificationPanel.Status = when (this) {
      Success -> EditorNotificationPanel.Status.Info
      else -> EditorNotificationPanel.Status.Error
    }
  }

  private fun createBorder(color: Color): CompoundBorder = BorderFactory.createCompoundBorder(
    RoundedLineBorder(
      color, NotificationBalloonRoundShadowBorderProvider.CORNER_RADIUS.get()
    ), JBUI.Borders.empty(BORDER_OFFSET)
  )

  companion object {
    private const val BORDER_OFFSET: Int = 10
  }
}