package com.jetbrains.edu.learning.newproject.ui.welcomeScreen

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.impl.DataManagerImpl
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.actions.CCNewCourseAction
import com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.NewHyperskillCourseAction
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction
import com.jetbrains.edu.learning.codeforces.StartCodeforcesContestAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.BrowseCoursesAction
import com.jetbrains.edu.learning.newproject.ui.GrayTextHtmlPanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.stepik.course.StartStepikCourseAction
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectAction
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Font
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

private const val EMPTY: String = "EMPTY"

@Suppress("unused") // will be used when implement MyCourses page
private const val CONTENT: String = "CONTENT"
private const val HEADER_FONT_SIZE = 26.0f
private const val MAIN_FONT_SIZE = 13.0f

class EduWelcomeTabPanel : JPanel() {
  private val cardLayout: CardLayout = CardLayout()
  private val moreActionsGroup = DefaultActionGroup(HyperskillProjectAction(),
                                                    ImportLocalCourseAction(),
                                                    StartStepikCourseAction(),
                                                    CCNewCourseAction(),
                                                    NewHyperskillCourseAction(),
                                                    StartCodeforcesContestAction())

  init {
    layout = cardLayout
    add(createEmptyPanel(), EMPTY)
    cardLayout.show(this, EMPTY)
  }

  private fun createEmptyPanel(): JPanel {
    val contentPanel = NonOpaquePanel(VerticalFlowLayout(VerticalFlowLayout.CENTER, 0, 0, false, false))
      .apply {
        border = JBUI.Borders.emptyTop(180)
        add(createHeaderPanel())
        add(createDescriptionPanel())
        add(createStartButtonPanel())
        add(createMoreActionsLink())
      }

    return JPanel().apply {
      background = MAIN_BG_COLOR
      add(contentPanel)
    }
  }

  /**
   * Inspired by `com.intellij.openapi.wm.impl.welcomeScreen.EmptyStateProjectsPanel.createLinkWithPopup`
   */
  private fun createMoreActionsLink(): LinkLabel<String> {
    return LinkLabel(EduCoreBundle.message("course.dialog.more.actions"), AllIcons.General.LinkDropTriangle, createLinkListener())
      .apply {
        horizontalTextPosition = SwingConstants.LEADING
        border = JBUI.Borders.emptyTop(14)
      }
  }

  private fun createLinkListener(): LinkListener<String> {
    return LinkListener { source, _ ->
      val popup = JBPopupFactory.getInstance().createActionGroupPopup(null,
                                                                      moreActionsGroup,
                                                                      DataManager.getInstance().getDataContext(source),
                                                                      JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                                                                      true)
      popup.showUnderneathOf(source)
    }
  }

  private fun createDescriptionPanel(): JComponent {
    val panel = GrayTextHtmlPanel(EduCoreBundle.message("course.dialog.welcome.tab.description"))
    panel.preferredSize = JBUI.size(373, 40)

    return panel
  }

  private fun createStartButtonPanel(): JPanel {
    val button = object : JButton(EduCoreBundle.message("course.dialog.welcome.screen.start.course")) {
      init {
        font = font.deriveFont(Font.BOLD, MAIN_FONT_SIZE)
        preferredSize = JBUI.size(158, 37)
        isOpaque = false
      }

      override fun isDefaultButton(): Boolean = true
    }

    button.addActionListener { e ->
      val action = BrowseCoursesAction()
      val event = AnActionEvent(
        null,
        (DataManager.getInstance() as DataManagerImpl).getDataContextTest(this),
        ActionPlaces.WELCOME_SCREEN,
        PresentationFactory().getPresentation(action),
        ActionManager.getInstance(),
        e.modifiers
      )
      action.actionPerformed(event)
    }

    return NonOpaquePanel(BorderLayout()).apply {
      border = JBUI.Borders.emptyTop(50)
      add(button)
    }
  }

  private fun createHeaderPanel(): JPanel {
    val scaledFont = JBUIScale.scaleFontSize(HEADER_FONT_SIZE)
    val text = "<b>${EduCoreBundle.message("course.dialog.welcome.tab.title")}<b>"
    val label = JBLabel(UIUtil.toHtml("<span style='font-size: $scaledFont'>$text</span>"))
    label.isOpaque = false

    return NonOpaquePanel(BorderLayout()).apply {
      border = JBUI.Borders.emptyBottom(20)
      add(label)
    }
  }
}