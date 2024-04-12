package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.AnActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType
import com.jetbrains.edu.learning.ui.EduColors
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel


class LessonHeader : JPanel() {
  private val headerText: JLabel

  private val topPanelForProblems = Box.createHorizontalBox()

  init {
    layout = BoxLayout(this, BoxLayout.X_AXIS)
    border = JBEmptyBorder(12, 0, 12, 0)

    headerText = JBLabel().withFont(JBFont.medium())
    headerText.foreground = EduColors.taskToolWindowLessonLabel
    headerText.alignmentX = LEFT_ALIGNMENT

    val leftBox = Box.createHorizontalBox()
    leftBox.add(headerText)
    leftBox.add(Box.createHorizontalGlue())
    add(leftBox)
    add(topPanelForProblems)
  }

  fun setHeaderText(header: String?) {
    headerText.text = header
  }

  fun updateTopPanelForProblems(project: Project, course: HyperskillCourse, task: Task) {
    topPanelForProblems.removeAll()
    if (CCUtils.isCourseCreator(project) || course.getProjectLesson() == null) return

    val linkText = if (course.isTaskInProject(task)) EduCoreBundle.message("hyperskill.back.to.learning") else EduCoreBundle.message("hyperskill.work.on.project")
    val action = if (course.isTaskInProject(task)) NavigateToUnsolvedTopic(project, course) else NavigateToProjectAction(project, course)
    val actionLink = AnActionLink(linkText, action).apply {
      font = JBFont.medium()
    }
    topPanelForProblems.add(Box.createHorizontalGlue())
    topPanelForProblems.add(actionLink)
  }


  private class NavigateToProjectAction(
    private val project: Project,
    private val course: HyperskillCourse
  ) : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val lesson = course.getProjectLesson() ?: return
      val currentTask = lesson.currentTask() ?: return
      NavigationUtils.navigateToTask(project, currentTask)
    }
  }

  private class NavigateToUnsolvedTopic(
    private val project: Project,
    private val course: HyperskillCourse
  ) : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
      val topicsSection = course.getTopicsSection()
      if (topicsSection != null) {
        val tasks = topicsSection.lessons.flatMap { it.taskList }
        val task = tasks.find { !it.isSolved } ?: tasks.last()
        NavigationUtils.navigateToTask(project, task)
      }
      else {
        TaskToolWindowView.getInstance(project).selectTab(TabType.TOPICS_TAB)
      }
    }
  }
}