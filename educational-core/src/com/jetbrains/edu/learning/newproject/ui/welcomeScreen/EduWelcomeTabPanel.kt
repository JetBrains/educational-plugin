package com.jetbrains.edu.learning.newproject.ui.welcomeScreen

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.PresentationFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.actions.CCNewCourseAction
import com.jetbrains.edu.coursecreator.actions.stepik.hyperskill.NewHyperskillCourseAction
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.codeforces.StartCodeforcesContestAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.BrowseCoursesAction
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseAddedListener
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseDeletedListener
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.CoursesDialogFontManager
import com.jetbrains.edu.learning.newproject.ui.GrayTextHtmlPanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Font
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants


private const val EMPTY = "empty"
private const val MY_COURSES_PANEL = "my-courses"

class EduWelcomeTabPanel(parentDisposable: Disposable) : JBScrollPane() {
  private val moreActionsGroup = DefaultActionGroup(CCNewCourseAction(), NewHyperskillCourseAction(), StartCodeforcesContestAction())
  private val cardLayout: CardLayout = CardLayout()
  private val mainPanel = JPanel(cardLayout)

  init {
    val welcomeScreenPanel = MyCoursesWelcomeScreenPanel(parentDisposable)
    mainPanel.add(welcomeScreenPanel, MY_COURSES_PANEL)
    mainPanel.add(createEmptyPanel(), EMPTY)
    setViewportView(mainPanel)

    showPanel()
    subscribeToCoursesStorageEvents(parentDisposable) { welcomeScreenPanel.updateModel() }
  }

  private fun subscribeToCoursesStorageEvents(disposable: Disposable, updateModel: () -> Unit) {
    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(CoursesStorage.COURSE_DELETED, object : CourseDeletedListener {
      override fun courseDeleted(course: Course) {
        updateModel()
        showPanel()
      }
    })
    connection.subscribe(CoursesStorage.COURSE_ADDED, object : CourseAddedListener {
      override fun courseAdded(course: Course) {
        updateModel()
        showPanel()
      }
    })
  }

  private fun showPanel() {
    if (CoursesStorage.getInstance().isNotEmpty()) {
      cardLayout.show(mainPanel, MY_COURSES_PANEL)
    }
    else {
      cardLayout.show(mainPanel, EMPTY)
    }
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
      val popup = JBPopupFactory.getInstance().createActionGroupPopup(
        null,
        moreActionsGroup,
        DataManager.getInstance().getDataContext(source),
        JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
        true
      )
      popup.showUnderneathOf(source)
    }
  }

  private fun createDescriptionPanel(): JComponent {
    val linkText = "<a href=${EduNames.LEARNER_START_GUIDE}>${EduCoreBundle.message("course.dialog.learn.more")}</a>"
    val text = "${EduCoreBundle.message("course.dialog.welcome.tab.description")} $linkText"
    val panel = GrayTextHtmlPanel(text, "text-align:center")
    panel.preferredSize = JBUI.size(330, 50)

    return panel
  }

  private fun createStartButtonPanel(): JPanel {
    val button = object : JButton(EduCoreBundle.message("course.dialog.welcome.screen.start.course")) {
      init {
        font = font.deriveFont(Font.BOLD, CoursesDialogFontManager.fontSize.toFloat())
        preferredSize = JBUI.size(158, 37)
        isOpaque = false
      }

      override fun isDefaultButton(): Boolean = true
    }

    button.addActionListener { e ->
      val action = BrowseCoursesAction()
      val event = AnActionEvent(
        null,
        DataManager.getInstance().getDataContext(this),
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
    val scaledFont = CoursesDialogFontManager.headerFontSize
    val text = "<b>${EduCoreBundle.message("course.dialog.welcome.tab.title")}<b>"
    val label = JBLabel(UIUtil.toHtml("<span style='font-size: $scaledFont'>$text</span>"))
    label.isOpaque = false

    return NonOpaquePanel(BorderLayout()).apply {
      border = JBUI.Borders.emptyBottom(20)
      add(label)
    }
  }
}
