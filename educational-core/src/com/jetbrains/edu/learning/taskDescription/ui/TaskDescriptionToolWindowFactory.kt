package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.editor.colors.FontPreferences
import com.intellij.openapi.options.FontSize
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.GotItTooltip
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.coursecreator.actions.CCEditTaskDescription
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.actions.PreviousTaskAction
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.actions.CodeforcesShowLoginStatusAction
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import java.awt.MouseInfo
import java.awt.Point
import javax.swing.JSlider
import javax.swing.SwingConstants
import javax.swing.event.ChangeListener

class TaskDescriptionToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun isApplicable(project: Project): Boolean = EduUtils.isEduProject(project)

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    if (!EduUtils.isEduProject(project)) {
      return
    }
    val taskDescriptionToolWindow = TaskDescriptionView.getInstance(project)
    toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")
    toolWindow.initTitleActions()
    addGotItTooltip(project)
    taskDescriptionToolWindow.init(toolWindow)
    (toolWindow as ToolWindowEx).setAdditionalGearActions(DefaultActionGroup(AdjustFontSize(project)))
  }

  private fun ToolWindow.initTitleActions() {
    val actions = arrayOf(CCEditTaskDescription.ACTION_ID, PreviousTaskAction.ACTION_ID, NextTaskAction.ACTION_ID,
                          CodeforcesShowLoginStatusAction.ACTION_ID).map {
      ActionManager.getInstance().getAction(it) ?: error("Action $it not found")
    }
    setTitleActions(actions)
  }

  private fun addGotItTooltip(project: Project) {
    val action = EduActionUtils.getAction(CodeforcesShowLoginStatusAction.ACTION_ID)
    val gotItTooltip = GotItTooltip("login.to.codeforces", EduCoreBundle.message("codeforces.login.to.codeforces.tooltip"), project)
    gotItTooltip.assignTo(action.templatePresentation, GotItTooltip.BOTTOM_MIDDLE)
    val jComponent = action.templatePresentation.getClientProperty(CustomComponentAction.COMPONENT_KEY)
    if (jComponent != null && gotItTooltip.canShow() && !CodeforcesSettings.getInstance().isLoggedIn()) {
      gotItTooltip.show(jComponent, GotItTooltip.BOTTOM_LEFT)
    }
  }

  /**
   * Items from slider are mapped to FontSize in reversed order as they are used as dividers in TypographyManager
   */
  private fun Int.toReverseIndex() = FontSize.values().size - 1 - this

  private inner class AdjustFontSize(private val project: Project) : DumbAwareAction
                                                                     (EduCoreBundle.message("action.adjust.font.size.text")) {
    override fun actionPerformed(e: AnActionEvent) {
      val fontSizeSlider = JSlider(SwingConstants.HORIZONTAL, 0, FontSize.values().size - 1, getInitialIndex())
      fontSizeSlider.minorTickSpacing = 1
      fontSizeSlider.paintTicks = true
      fontSizeSlider.paintTrack = true
      fontSizeSlider.snapToTicks = true
      UIUtil.setSliderIsFilled(fontSizeSlider, true)
      fontSizeSlider.addChangeListener(ChangeListener {
        val fontFactor = FontSize.values()[fontSizeSlider.value.toReverseIndex()]
        PropertiesComponent.getInstance().setValue(StyleManager.FONT_SIZE_PROPERTY, fontFactor.size, FontPreferences.DEFAULT_FONT_SIZE)
        if (!(EduUtils.getCurrentTask(project) is VideoTask && isJCEF())) {
          TaskDescriptionView.updateAllTabs(project)
        }
      })
      val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(fontSizeSlider, fontSizeSlider).createPopup()
      val location = MouseInfo.getPointerInfo().location
      popup.show(RelativePoint(
        Point(location.x - fontSizeSlider.preferredSize.width, location.y + fontSizeSlider.preferredSize.height)))
    }

    private fun getInitialIndex(): Int {
      val value = PropertiesComponent.getInstance().getInt(StyleManager.FONT_SIZE_PROPERTY, FontPreferences.DEFAULT_FONT_SIZE)
      for ((i, fontSize) in FontSize.values().withIndex()) {
        if (fontSize.size == value) {
          return i.toReverseIndex()
        }
      }
      return FontSize.values().size / 2
    }
  }

  override fun init(window: ToolWindow) {
    window.setIcon(EducationalCoreIcons.CourseToolWindow)
  }

  companion object {
    const val STUDY_TOOL_WINDOW = "Task"
  }
}
