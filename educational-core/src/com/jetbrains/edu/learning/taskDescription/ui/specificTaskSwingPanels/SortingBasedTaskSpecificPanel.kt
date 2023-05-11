package com.jetbrains.edu.learning.taskDescription.ui.specificTaskSwingPanels

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.Gaps
import com.intellij.ui.dsl.gridLayout.VerticalGaps
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.MatchingTaskUI
import com.jetbrains.edu.learning.taskDescription.ui.addBorder
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.text.JTextComponent
import kotlin.math.max

class SortingBasedTaskSpecificPanel(task: SortingBasedTask) : Wrapper() {
  private val emptyBorder = JBUI.Borders.empty(8, 12, 8, 6)
  private val roundedBorderSize = 1

  private val arcSize = 8

  private val rowGapSize = 12
  private val columnGapSize = 8

  private val codeFont = Font.decode(StyleManager().codeFont)

  private val valueTextComponents = mutableListOf<JTextComponent>()

  init {
    val panel = panel {
      for (index in task.ordering.indices) {
        createOption(task, index)
          .layout(RowLayout.PARENT_GRID)
          .customize(VerticalGaps(bottom = rowGapSize))
      }
      align(Align.FILL)
    }.apply {
      isOpaque = false
      border = JBUI.Borders.empty(15, 0, 10, 10)
    }
    setContent(panel)
  }

  private fun Panel.createOption(task: SortingBasedTask, index: Int): Row = row {
    val indexPanel = createIndexPanel(task, index)
      ?.addBorder(emptyBorder)
      ?.addBorder(RoundedLineBorder(MatchingTaskUI.Key.background(), arcSize, roundedBorderSize))
      ?.apply {
        foreground = MatchingTaskUI.Key.foreground()
        background = MatchingTaskUI.Key.background()
      }

    val optionPanel = createOptionPanel(task, index)
      .addBorder(emptyBorder)
      .addBorder(RoundedLineBorder(MatchingTaskUI.Value.borderColor(), arcSize, roundedBorderSize))
      .apply {
        foreground = MatchingTaskUI.Value.foreground()
        background = MatchingTaskUI.Value.background()
      }

    syncHeight(indexPanel, optionPanel)

    if (indexPanel != null) {
      cell(RoundedWrapper(indexPanel, arcSize))
        .widthGroup("IndexPanel")
        .customize(Gaps(right = columnGapSize))
    }

    cell(RoundedWrapper(optionPanel, arcSize))
      .align(AlignX.FILL)
      .resizableColumn()
  }

  private fun syncHeight(panel1: JPanel?, panel2: JPanel?) {
    if (panel1 == null || panel2 == null) return
    val height1 = panel1.preferredSize.height
    val height2 = panel2.preferredSize.height
    val maxHeight = max(height1, height2)
    val dh1 = maxHeight - height1
    val dh2 = maxHeight - height2
    panel1.addBorder(JBUI.Borders.empty(dh1 / 2, 0, dh1 / 2 + dh1 % 2, 0))
    panel2.addBorder(JBUI.Borders.empty(dh2 / 2, 0, dh2 / 2 + dh2 % 2, 0))
  }

  private fun createIndexPanel(task: SortingBasedTask, index: Int): DialogPanel? {
    if (task !is MatchingTask) return null
    return panel {
      row {
        text(task.captions[index])
          .align(AlignY.CENTER)
          .apply {
            component.font = Font(codeFont.name, codeFont.style, 13)
          }
      }
    }
  }

  private fun createOptionPanel(task: SortingBasedTask, index: Int): DialogPanel {
    return panel {
      row {
        val optionIndex = task.ordering[index]
        val textComponent = text(task.options[optionIndex])
          .align(AlignY.CENTER)
          .resizableColumn()
          .apply {
            component.font = Font(codeFont.name, codeFont.style, 13)
          }
          .component
        valueTextComponents += textComponent
        cell(createNavigationsButtonsPanel(task, index))
          .align(AlignY.CENTER)
          .align(AlignX.RIGHT)
      }
      resizableColumn()
    }
  }

  private fun createNavigationsButtonsPanel(task: SortingBasedTask, index: Int): JComponent {
    return NonOpaquePanel(VerticalFlowLayout(0, 0)).apply {
      addActionButton(createUpAction(task, index))
      addActionButton(createDownAction(task, index))
      border = JBUI.Borders.empty()
    }
  }

  private fun NonOpaquePanel.addActionButton(action: DumbAwareAction) {
    val actionButton = ActionButton(
      action,
      action.templatePresentation.clone(),
      "unknown",
      ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    ).apply {
      border = JBUI.Borders.empty()
    }
    add(actionButton)
  }

  private fun moveUp(task: SortingBasedTask, index: Int) {
    task.moveOptionUp(index)

    valueTextComponents[index].text = task.options[task.ordering[index]]
    valueTextComponents[index - 1].text = task.options[task.ordering[index - 1]]
  }

  private fun moveDown(task: SortingBasedTask, index: Int) {
    task.moveOptionDown(index)

    valueTextComponents[index].text = task.options[task.ordering[index]]
    valueTextComponents[index + 1].text = task.options[task.ordering[index + 1]]
  }

  private fun createUpAction(task: SortingBasedTask, index: Int): DumbAwareAction {
    return object : DumbAwareAction(
      EduCoreBundle.lazyMessage("sorting.based.task.move.up"),
      EduCoreBundle.lazyMessage("sorting.based.task.move.up.description"),
      EducationalCoreIcons.MoveUpMatching
    ) {
      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

      override fun actionPerformed(e: AnActionEvent) = moveUp(task, index)

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = index > 0
      }
    }
  }

  private fun createDownAction(task: SortingBasedTask, index: Int): DumbAwareAction {
    return object : DumbAwareAction(
      EduCoreBundle.lazyMessage("sorting.based.task.move.down"),
      EduCoreBundle.lazyMessage("sorting.based.task.move.down.description"),
      EducationalCoreIcons.MoveDownMatching
    ) {
      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

      override fun actionPerformed(e: AnActionEvent) = moveDown(task, index)

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = index + 1 < task.options.size
      }
    }
  }
}