package com.jetbrains.edu.learning.newproject.ui.welcomeScreen

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionButtonLook
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.IdeaActionButtonLook
import com.intellij.openapi.actionSystem.impl.Win10ActionButtonLook
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.OnePixelDivider
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.actions.CCNewCourseAction
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction
import com.jetbrains.edu.learning.codeforces.StartCodeforcesContestAction
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.BrowseCoursesAction
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.newproject.ui.coursePanel.OpenCourseButton
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesListPanel
import com.jetbrains.edu.learning.newproject.ui.filters.CoursesFilterComponent
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.EduWelcomeTabPanel.Companion.IS_FROM_WELCOME_SCREEN
import java.awt.BorderLayout
import java.awt.Graphics
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel


private const val ACTION_PLACE = "MyCoursesWelcomeTab"

class MyCoursesWelcomeScreenPanel(disposable: Disposable) : JPanel(BorderLayout()) {
  private val coursesListPanel = CoursesListPanel {
    coursesFilterComponent.resetSearchField()
    updateModel(createCoursesGroup())
  }

  private val coursesFilterComponent: CoursesFilterComponent = CoursesFilterComponent({ createCoursesGroup() },
                                                                                      { group -> updateModel(group) })

  init {
    background = MAIN_BG_COLOR

    coursesListPanel.border = JBUI.Borders.emptyTop(8)
    coursesListPanel.setClickListener {
      OpenCourseButton.openCourse(it, this)
      true
    }
    add(coursesListPanel, BorderLayout.CENTER)

    val searchComponent = createSearchComponent(disposable)
    add(searchComponent, BorderLayout.NORTH)

    updateModel(createCoursesGroup())
  }

  private fun createCoursesGroup(): List<CoursesGroup> {
    val coursesGroups = CoursesStorage.getInstance().coursesInGroups()
    coursesGroups.flatMap { it.courses }.forEach {
      it.dataHolder.putUserData(IS_FROM_WELCOME_SCREEN, true)
    }
    return coursesGroups
  }


  private fun createSearchComponent(disposable: Disposable): JPanel {
    val panel = NonOpaquePanel()
    val searchField = coursesFilterComponent
    UIUtil.setBackgroundRecursively(searchField, MAIN_BG_COLOR)

    panel.add(searchField, BorderLayout.CENTER)
    panel.add(createActionToolbar(panel), BorderLayout.EAST)
    panel.border = JBUI.Borders.empty(8, 6, 8, 10)

    ApplicationManager.getApplication().messageBus.connect(disposable)
      .subscribe(LafManagerListener.TOPIC, LafManagerListener {
        UIUtil.setBackgroundRecursively(panel, MAIN_BG_COLOR)
        searchField.removeBorder()
      })

    return Wrapper(panel).apply {
      border = JBUI.Borders.customLine(OnePixelDivider.BACKGROUND, 0, 0, 1, 0)
    }
  }

  private fun createActionToolbar(parent: NonOpaquePanel): JComponent {
    val browseCoursesAction = BrowseCoursesAction()

    val button = JButton(EduCoreBundle.message("course.dialog.start.new.course")).apply {
      isOpaque = false
      addActionListener(ActionListener {
        val dataContext = DataManager.getInstance().getDataContext(parent)
        browseCoursesAction.actionPerformed(AnActionEvent.createFromAnAction(browseCoursesAction, null, ACTION_PLACE, dataContext))
      })
    }

    return NonOpaquePanel().apply {
      add(button, BorderLayout.LINE_START)
      add(createMoreActionsButton(), BorderLayout.LINE_END)
    }
  }

  fun updateModel() {
    coursesListPanel.updateModel(createCoursesGroup(), null)
  }

  private fun updateModel(coursesGroup: List<CoursesGroup>) {
    coursesListPanel.updateModel(coursesGroup, null)
  }

  private fun createMoreActionsButton(): JComponent {
    val moreActionGroup = DefaultActionGroup("", true)
    moreActionGroup.addAll(CCNewCourseAction(EduCoreBundle.message("course.dialog.create.course.title")),
                           ImportLocalCourseAction(),
                           StartCodeforcesContestAction())

    val moreActionPresentation = moreActionGroup.templatePresentation
    moreActionPresentation.icon = AllIcons.Actions.More
    moreActionPresentation.putClientProperty(ActionButton.HIDE_DROPDOWN_ICON, true)

    return ActionButton(moreActionGroup, moreActionPresentation, ACTION_PLACE, JBUI.size(12, 12)).apply {
      setLook(ActionButtonLookWithHover())
    }
  }

  private class ActionButtonLookWithHover : ActionButtonLook() {
    private var delegate: ActionButtonLook = if (UIUtil.isUnderWin10LookAndFeel()) Win10ActionButtonLook() else IdeaActionButtonLook()

    override fun paintBackground(g: Graphics?, component: JComponent?, state: Int) {
      delegate.paintBackground(g, component, state)
    }
  }
}
