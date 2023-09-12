package com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionButtonLook
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.actionSystem.impl.IdeaActionButtonLook
import com.intellij.openapi.actionSystem.impl.Win10ActionButtonLook
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.LabeledIcon
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import com.jetbrains.edu.learning.taskToolWindow.ui.LightColoredActionLink
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.check.CheckPanel
import com.jetbrains.edu.learning.ui.EduColors
import org.jdesktop.swingx.icon.EmptyIcon
import java.awt.*
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*

class NavigationMapPanel : JPanel(BorderLayout()) {
  private val headerText: JLabel
  private val defaultActionGroup = DefaultActionGroup()
  private val toolbar: ActionToolbarImpl = ActionToolbarImpl(CheckPanel.ACTION_PLACE, defaultActionGroup, true)
  private val topPanelForProblems: JPanel = JPanel().apply {
    background = TaskToolWindowView.getTaskDescriptionBackgroundColor()
    maximumSize = JBUI.size(Int.MAX_VALUE, 30)
  }

  init {
    headerText = JBLabel().withBorder(JBEmptyBorder(12, 0, 6, 0))
    headerText.fontColor = UIUtil.FontColor.BRIGHTER
    toolbar.targetComponent = this
    toolbar.component.border = JBEmptyBorder(0, 0, 10, 0)
    toolbar.setCustomButtonLook(MyActionButtonLook())
    defaultActionGroup.isSearchable = false
    add(headerText, BorderLayout.WEST)
    add(topPanelForProblems, BorderLayout.EAST)
    add(toolbar.component, BorderLayout.SOUTH)
  }

  fun setHeader(header: String) {
    headerText.text = header
  }

  fun replaceActions(actionList: List<AnAction>) {
    defaultActionGroup.removeAll()
    defaultActionGroup.addAll(actionList)
  }

  fun updateTopPanelForProblems(project: Project, course: HyperskillCourse, task: Task) {
    topPanelForProblems.removeAll()
    if (course.isTaskInProject(task) || CCUtils.isCourseCreator(project) || course.getProjectLesson() == null) {
      return
    }
    val actionLink = LightColoredActionLink(
      EduCoreBundle.message("hyperskill.work.on.project"),
      NavigateToProjectAction(project, course)
    ).apply {
      border = JBUI.Borders.empty(12, 0, 6, 0)
    }
    topPanelForProblems.add(actionLink, BorderLayout.NORTH)
    topPanelForProblems.add(JSeparator(), BorderLayout.SOUTH)
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

  private class MyActionButtonLook : ActionButtonLook() {
    private var delegate: ActionButtonLook = if (UIUtil.isUnderWin10LookAndFeel()) Win10ActionButtonLook() else IdeaActionButtonLook()

    override fun paintBorder(g: Graphics?, component: JComponent?, state: Int) {
      g ?: return
      val action = (component as ActionButton).action
      if (action is NavigationMapAction) {

        val rect = Rectangle(component.getSize())
        JBInsets.removeFrom(rect, component.getInsets())

        val isSelected = action.isSelected
        val color = if (isSelected) EduColors.navigationMapIconSelectedBorder else action.task.navMapBorderColor
        if (!isSelected) {
          paintLookBorder(g, rect, color)
        }
        else {
          // Copied from com.intellij.openapi.actionSystem.impl.IdeaActionButtonLook.paintLookBorder to paint bold border
          // when the task is selected
          val g2 = g.create() as Graphics2D
          g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
          g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)

          try {
            g2.color = color
            val arc = DarculaUIUtil.BUTTON_ARC.float
            val lw = DarculaUIUtil.LW.float * 2
            val border = Path2D.Float(Path2D.WIND_EVEN_ODD)
            border.append(
              RoundRectangle2D.Float(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat(), arc, arc),
              false
            )
            border
              .append(
                RoundRectangle2D.Float(
                  rect.x + lw,
                  rect.y + lw,
                  rect.width - lw * 2,
                  rect.height - lw * 2,
                  arc - lw / 2,
                  arc - lw / 2
                ), false
              )
            g2.fill(border)
          }
          finally {
            g2.dispose()
          }
        }
      }
    }

    override fun paintLookBackground(g: Graphics, rect: Rectangle, color: Color) {
      delegate.paintLookBackground(g, rect, color)
    }

    override fun paintLookBorder(g: Graphics, rect: Rectangle, color: Color) {
      delegate.paintLookBorder(g, rect, color)
    }
  }
}

class NavigationMapAction(val task: Task, private val currentTask: Task, private val index: Int) : DumbAwareAction() {

  var isSelected = false

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (isSelected) return
    NavigationUtils.navigateToTask(project, task, currentTask)
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    isSelected = e.project?.getCurrentTask() == task
    presentation.isEnabled = true
    if ((task.index > 1) && isPreviousTaskUnsolvedHyperskillStage(task)) {
      presentation.isEnabled = false
    }
    presentation.text = task.presentableName

    presentation.icon = if (task is TheoryTask) {
      EducationalCoreIcons.TheoryTask
    }
    else {
      val labeledIcon = LabeledIcon(EmptyIcon(), index.toString(), "")
      labeledIcon.iconTextGap = 2
      labeledIcon
    }
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  private fun isPreviousTaskUnsolvedHyperskillStage(task: Task): Boolean =
    NavigationUtils.isUnsolvedHyperskillStage(task.lesson.taskList[task.index - 2])
}

private val Task.navMapBorderColor: JBColor
  get() {
    return when (this) {
      is TheoryTask -> EduColors.navigationMapIconSolvedBorder
      else -> {
        if (isSolved) EduColors.navigationMapIconSolvedBorder else EduColors.navigationMapIconNotSelectedBorder
      }
    }
  }
