package com.jetbrains.edu.learning.ui.taskDescription

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.colors.FontPreferences
import com.intellij.openapi.options.FontSize
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.actions.CCEditTaskDescription
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.ui.taskDescription.styleManagers.StyleManager
import icons.EducationalCoreIcons
import java.awt.MouseInfo
import java.awt.Point
import javax.swing.JSlider
import javax.swing.SwingConstants
import javax.swing.event.ChangeListener

class TaskDescriptionToolWindowFactory : ToolWindowFactory, DumbAware {

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    if (!EduUtils.isStudyProject(project)) {
      return
    }
    toolWindow.icon = EducationalCoreIcons.CourseToolWindow
    val taskDescriptionToolWindow = TaskDescriptionView.getInstance(project)
    toolWindow.initTitleActions()
    taskDescriptionToolWindow.init(toolWindow)
    (toolWindow as ToolWindowEx).setAdditionalGearActions(DefaultActionGroup(AdjustFontSize(taskDescriptionToolWindow)))
  }

  private fun ToolWindow.initTitleActions() {
    val actions = arrayOf(CCEditTaskDescription.ACTION_ID, PreviousTaskAction.ACTION_ID, NextTaskAction.ACTION_ID).map {
      ActionManager.getInstance().getAction(it) ?: error("Action $it not found")
    }.toTypedArray()
    (this as ToolWindowEx).setTitleActions(*actions)
  }

  private class AdjustFontSize(private val taskDescription: TaskDescriptionView) : DumbAwareAction("Adjust font size...") {
    override fun actionPerformed(e: AnActionEvent) {
      val fontSizeSlider = JSlider(SwingConstants.HORIZONTAL, 0, FontSize.values().size - 1, getInitialIndex())
      fontSizeSlider.minorTickSpacing = 1
      fontSizeSlider.paintTicks = true
      fontSizeSlider.paintTrack = true
      fontSizeSlider.snapToTicks = true
      UIUtil.setSliderIsFilled(fontSizeSlider, true)
      fontSizeSlider.addChangeListener(ChangeListener {
        //items from slider are mapped to FontSize in reversed order as they are used as divider in TypographyManager
        val fontFactor = FontSize.values()[FontSize.values().size - 1 - fontSizeSlider.value]
        PropertiesComponent.getInstance().setValue(StyleManager.FONT_FACTOR_PROPERTY, fontFactor.size, FontPreferences.DEFAULT_FONT_SIZE)
        taskDescription.updateTaskDescription()
      })
      val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(fontSizeSlider, fontSizeSlider).createPopup()
      val location = MouseInfo.getPointerInfo().location
      popup.show(RelativePoint(
        Point(location.x - fontSizeSlider.preferredSize.width, location.y + fontSizeSlider.preferredSize.height)))
    }

    private fun getInitialIndex(): Int {
      val value = PropertiesComponent.getInstance().getInt(StyleManager.FONT_FACTOR_PROPERTY, FontPreferences.DEFAULT_FONT_SIZE)
      for ((i, size) in FontSize.values().withIndex()) {
        if (size.size == value) {
          //items from slider are mapped to FontSize in reversed order as they are used as divider in TypographyManager
          return FontSize.values().size - 1 - i
        }
      }
      return FontSize.values().size / 2
    }
  }

  companion object {
    const val STUDY_TOOL_WINDOW = "Task"
  }
}
