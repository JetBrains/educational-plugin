package com.jetbrains.edu.aiHints.core.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.IdeBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.ui.*
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesCounterUsageCollector
import com.jetbrains.edu.ai.translation.statistics.EduAIFeaturesEventFields.HintBannerType
import com.jetbrains.edu.ai.translation.ui.LikeBlock.FeedbackLikenessAnswer
import com.jetbrains.edu.aiHints.core.log.Logger
import com.jetbrains.edu.aiHints.core.messages.EduAIHintsCoreBundle
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.ui.EduColors
import org.jetbrains.annotations.Nls
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.CompoundBorder

/**
 * The implementation has been adopted from the Intellij's platform [com.intellij.ui.InlineBanner] so to support adding custom actions.
 */
open class HintInlineBanner(
  private val project: Project,
  protected val task: Task,
  @Nls messageText: String,
  status: Status = Status.Success,
  gap: Int = JBUI.scale(8)
) : InlineBannerBase(status.toNotificationBannerStatus(), gap, messageText) {
  private var closeAction: Runnable? = null
  private val linkActionPanel: JPanel = JPanel(HorizontalLayout(JBUI.scale(16)))
  private val iconActionPanel: JPanel = JPanel(HorizontalLayout(JBUI.scale(16)))
  private val actionsPanel: JPanel = JPanel(BorderLayout())
  var likeness = FeedbackLikenessAnswer.NO_ANSWER
    private set

  init {
    val myCloseButton = createInplaceCloseButton {
      close()
    }

    layout = object : BorderLayout(gap, gap) {
      @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
      override fun addLayoutComponent(name: String?, comp: Component) {
        if (comp !== myCloseButton) {
          super.addLayoutComponent(name, comp)
        }
      }

      override fun layoutContainer(target: Container) {
        super.layoutContainer(target)
        val y = JBUI.scale(7)
        var x = target.width - JBUI.scale(7)
        if (myCloseButton.isVisible) {
          val size = myCloseButton.preferredSize
          x -= size.width
          myCloseButton.setBounds(x, y, size.width, size.height)
          x -= JBUI.scale(2)
        }
      }
    }

    setIcon()
    add(iconPanel, BorderLayout.WEST)
    add(centerPanel)
    add(myCloseButton)
    setTitle()
    setActions()
    isOpaque = false
    toolTipText = status.toolTipText
    border = createBorder(status.borderColor)
    background = status.backgroundColor
    closeAction = Runnable {
      EduAIFeaturesCounterUsageCollector.hintBannerClosed(task)
      Logger.aiHintsLogger.info(
        """|| Course id: ${task.course.id} | Lesson id: ${task.lesson.id} | Task id: ${task.id}
           || Action: hint banner is closed
        """.trimMargin()
      )
    }
  }

  final override fun add(comp: Component?): Component {
    return super.add(comp)
  }

  final override fun add(comp: Component, constraints: Any?) {
    super.add(comp, constraints)
  }

  @RequiresEdt
  fun display() {
    val hintBannerType = when (this) {
      is CodeHintInlineBanner -> HintBannerType.CODE
      is TextHintInlineBanner -> HintBannerType.TEXT
      is ErrorHintInlineBanner -> HintBannerType.ERROR
      else -> error("Unexpected hint banner type: ${javaClass.simpleName}")
    }
    EduAIFeaturesCounterUsageCollector.hintBannerShown(hintBannerType, task)
    Logger.aiHintsLogger.info(
      """|| Course id: ${task.course.id} | Lesson id: ${task.lesson.id} | Task id: ${task.id}
         || Action: hint banner is shown
         || Type: ${hintBannerType.name}
      """.trimMargin()
    )
    TaskToolWindowView.getInstance(project).addInlineBannerToCheckPanel(this@HintInlineBanner)
  }

  enum class Status(val backgroundColor: Color, val borderColor: Color, val toolTipText: String? = null) {
    Success(
      EduColors.aiGetHintInlineBannersBackgroundColor,
      EduColors.aiGetHintInlineBannersBorderColor,
      EduAIHintsCoreBundle.message("hints.label.ai.generated.content.tooltip")
    ),
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

  fun addAction(name: @Nls String, action: Runnable): InlineBannerBase {
    actionsPanel.isVisible = true
    linkActionPanel.add(object : LinkLabel<Runnable>(name, null, { _, action -> action.run() }, action) {
      override fun getTextColor() = JBUI.CurrentTheme.Link.Foreground.ENABLED
    }, linkActionPanel.componentCount - 1)
    return this
  }

  fun addLikeDislikeActions(action: () -> FeedbackLikenessAnswer) {
    actionsPanel.isVisible = true
    val likeLabel = JLabel(AllIcons.Ide.LikeDimmed)
    val dislikeLabel = JLabel(AllIcons.Ide.DislikeDimmed)
    likeLabel.setupToggleWith(dislikeLabel, AllIcons.Ide.LikeSelected, AllIcons.Ide.DislikeDimmed, FeedbackLikenessAnswer.LIKE, action)
    dislikeLabel.setupToggleWith(likeLabel, AllIcons.Ide.DislikeSelected, AllIcons.Ide.LikeDimmed, FeedbackLikenessAnswer.DISLIKE, action)
    iconActionPanel.add(likeLabel, iconActionPanel.componentCount - 1)
    iconActionPanel.add(dislikeLabel, iconActionPanel.componentCount - 1)
  }

  private fun JLabel.setupToggleWith(
    otherLabel: JLabel,
    selectedIcons: Icon,
    unselectedIcon: Icon,
    newLikeness: FeedbackLikenessAnswer,
    action: () -> FeedbackLikenessAnswer
  ) {
    preferredSize = JBDimension(26, 26)

    addMouseListener(object : MouseAdapter() {
      override fun mouseEntered(e: MouseEvent?) {
        background = JBUI.CurrentTheme.InlineBanner.HOVER_BACKGROUND
        isOpaque = true
        repaint()
      }

      override fun mousePressed(e: MouseEvent?) {
        background = JBUI.CurrentTheme.InlineBanner.PRESSED_BACKGROUND
        repaint()
      }

      override fun mouseReleased(e: MouseEvent?) {
        background = this@HintInlineBanner.background
        isOpaque = true
        repaint()
      }

      override fun mouseExited(e: MouseEvent?) {
        isOpaque = false
        repaint()
      }

      override fun mouseClicked(e: MouseEvent?) {
        updateIcons(this@setupToggleWith, otherLabel, selectedIcons, unselectedIcon)
        likeness = newLikeness
        val resultLikeness = action()
        if (resultLikeness == likeness) return

        when {
          resultLikeness == FeedbackLikenessAnswer.LIKE && likeness == FeedbackLikenessAnswer.LIKE -> {
            updateIcons(
              currentLabel = this@setupToggleWith,
              otherLabel = otherLabel,
              currentIcon = AllIcons.Ide.LikeSelected,
              otherIcon = AllIcons.Ide.DislikeDimmed
            )
          }

          resultLikeness == FeedbackLikenessAnswer.LIKE && likeness == FeedbackLikenessAnswer.DISLIKE -> {
            updateIcons(
              currentLabel = otherLabel,
              otherLabel = this@setupToggleWith,
              currentIcon = AllIcons.Ide.LikeSelected,
              otherIcon = AllIcons.Ide.DislikeDimmed
            )
          }

          resultLikeness == FeedbackLikenessAnswer.DISLIKE && likeness == FeedbackLikenessAnswer.DISLIKE -> {
            updateIcons(
              currentLabel = this@setupToggleWith,
              otherLabel = otherLabel,
              currentIcon = AllIcons.Ide.DislikeSelected,
              otherIcon = AllIcons.Ide.LikeDimmed
            )
          }

          resultLikeness == FeedbackLikenessAnswer.DISLIKE && likeness == FeedbackLikenessAnswer.LIKE -> {
            updateIcons(
              currentLabel = otherLabel,
              otherLabel = this@setupToggleWith,
              currentIcon = AllIcons.Ide.DislikeSelected,
              otherIcon = AllIcons.Ide.LikeDimmed
            )
          }

          resultLikeness == FeedbackLikenessAnswer.NO_ANSWER && likeness == FeedbackLikenessAnswer.LIKE -> {
            updateIcons(
              currentLabel = otherLabel,
              otherLabel = this@setupToggleWith,
              currentIcon = AllIcons.Ide.DislikeDimmed,
              otherIcon = AllIcons.Ide.LikeDimmed
            )
          }

          resultLikeness == FeedbackLikenessAnswer.NO_ANSWER && likeness == FeedbackLikenessAnswer.DISLIKE -> {
            updateIcons(
              currentLabel = this@setupToggleWith,
              otherLabel = otherLabel,
              currentIcon = AllIcons.Ide.DislikeDimmed,
              otherIcon = AllIcons.Ide.LikeDimmed
            )
          }
        }

        likeness = resultLikeness
      }
    })
  }

  private fun updateIcons(
    currentLabel: JLabel,
    otherLabel: JLabel,
    currentIcon: Icon,
    otherIcon: Icon
  ) {
    currentLabel.icon = currentIcon
    otherLabel.icon = otherIcon
    currentLabel.repaint()
  }

  private fun setIcon() {
    val myIcon = JBLabel()
    myIcon.icon = EduAiHintsIcons.Hint
    myIcon.isVisible = true
    iconPanel.isVisible = true
    iconPanel.add(myIcon, BorderLayout.NORTH)
  }

  private fun setTitle() {
    val titlePanel = JPanel(BorderLayout())
    titlePanel.isOpaque = isOpaque
    titlePanel.background = background
    titlePanel.add(message)
    centerPanel.add(titlePanel)

    val myButtonPanel = JPanel()
    myButtonPanel.preferredSize = JBDimension(22, 16)
    myButtonPanel.isOpaque = isOpaque
    titlePanel.add(myButtonPanel, BorderLayout.EAST)
  }

  private fun setActions() {
    linkActionPanel.isOpaque = isOpaque
    iconActionPanel.isOpaque = isOpaque
    actionsPanel.isOpaque = isOpaque
    actionsPanel.add(linkActionPanel, BorderLayout.WEST)
    actionsPanel.add(iconActionPanel, BorderLayout.EAST)
    centerPanel.add(actionsPanel)
  }

  fun close() {
    closeAction?.run()
    removeFromParent()
  }

  private fun removeFromParent() {
    val parent = parent
    parent?.remove(this)
    parent?.doLayout()
    parent?.revalidate()
    parent?.repaint()
  }

  private fun createInplaceCloseButton(listener: ActionListener): JComponent {
    val button = object : InplaceButton(IdeBundle.message("editor.banner.close.tooltip"), IconButton(null, AllIcons.General.Close, null, null), listener) {
      private val myTimer = Timer(300) { stopClickTimer() }
      private var myClick = false

      private fun startClickTimer() {
        myClick = true
        repaint()
        myTimer.start()
      }

      private fun stopClickTimer() {
        myClick = false
        repaint()
        myTimer.stop()
      }

      override fun doClick(e: MouseEvent) {
        startClickTimer()
        super.doClick(e)
      }

      override fun paintHover(g: Graphics) {
        paintHover(g, if (myClick) JBUI.CurrentTheme.InlineBanner.PRESSED_BACKGROUND else JBUI.CurrentTheme.InlineBanner.HOVER_BACKGROUND)
      }
    }
    button.preferredSize = JBDimension(26, 26)
    button.isVisible = true
    return button
  }

  companion object {
    private const val BORDER_OFFSET: Int = 10
  }
}