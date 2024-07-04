package com.jetbrains.edu.learning.taskToolWindow.ui.navigationMap

import com.intellij.ide.ui.laf.darcula.DarculaUIUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionButtonLook
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBInsets
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.projectView.CourseViewUtils.isSolved
import com.jetbrains.edu.learning.ui.EduColors
import java.awt.*
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*

class NavigationMapToolbar(private val defaultActionGroup: DefaultActionGroup = DefaultActionGroup()) : ActionToolbarImpl(
  ACTION_PLACE,
  defaultActionGroup,
  true
) {

  init {
    // '-4' to align the left border
    border = JBEmptyBorder(8, -4, 20, 8)
    layoutStrategy = ToolbarLayoutStrategy.NOWRAP_STRATEGY
    setCustomButtonLook(MyActionButtonLook())
    setMinimumButtonSize(Dimension(28, 28))
    setActionButtonBorder(4, 0)
    defaultActionGroup.isSearchable = false
  }

  fun replaceActions(actionList: List<AnAction>) {
    defaultActionGroup.removeAll()
    defaultActionGroup.addAll(actionList)
  }

  // Copied from com.intellij.openapi.actionSystem.impl.IdeaActionButtonLook
  // to paint bold border, custom round corners and colors
  private class MyActionButtonLook : ActionButtonLook() {
    private val buttonArk = 8f

    override fun paintBorder(g: Graphics?, component: JComponent?, state: Int) {
      g ?: return
      val action = (component as ActionButton).action
      if (action is NavigationMapAction) {

        val rect = Rectangle(component.getSize())
        JBInsets.removeFrom(rect, component.getInsets())

        val isSelected = action.isSelected
        val color = when {
          !component.isEnabled -> EduColors.navigationMapDisabledIconBackground
          isSelected -> EduColors.navigationMapIconSelectedBorder
          else -> if (action.task.isSolved) EduColors.navigationMapIconSolvedBorder else EduColors.navigationMapIconNotSelectedBorder
        }

        val g2 = g.create() as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)

        try {
          g2.color = color
          val arc = buttonArk
          val lw = if (isSelected) DarculaUIUtil.LW.float * 2 else DarculaUIUtil.LW.float
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

    override fun paintBackground(g: Graphics?, component: JComponent?, state: Int) {
      if (component != null && !component.isEnabled) {
        paintBackground(g, component, EduColors.navigationMapDisabledIconBackground)
      }
      else {
        super.paintBackground(g, component, state)
      }
    }

    override fun paintLookBackground(g: Graphics, rect: Rectangle, color: Color) {
      val g2 = g.create() as Graphics2D
      try {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
        g2.color = color
        val arc: Float = buttonArk
        g2.fill(RoundRectangle2D.Float(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat(), arc, arc))
      }
      finally {
        g2.dispose()
      }
    }

  }

  companion object {
    const val ACTION_PLACE = "NavigationMap"
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
    presentation.icon = if (task is TheoryTask && task.course is HyperskillCourse) {
      EducationalCoreIcons.NavigationMapTheoryTask
    }
    else {
      EduTextIcon(index.toString())
    }
    presentation.disabledIcon = EduTextIcon(index.toString())
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  private fun isPreviousTaskUnsolvedHyperskillStage(task: Task): Boolean =
    NavigationUtils.isUnsolvedHyperskillStage(task.lesson.taskList[task.index - 2])
}
