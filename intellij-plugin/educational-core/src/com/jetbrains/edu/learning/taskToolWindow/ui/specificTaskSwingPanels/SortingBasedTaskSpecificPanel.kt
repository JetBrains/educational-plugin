package com.jetbrains.edu.learning.taskToolWindow.ui.specificTaskSwingPanels

import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.ui.dsl.gridLayout.UnscaledGapsY
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.asCssColor
import com.jetbrains.edu.learning.taskToolWindow.ui.MatchingTaskUI
import com.jetbrains.edu.learning.taskToolWindow.ui.addBorder
import com.jetbrains.edu.learning.taskToolWindow.ui.getSortingShortcutHTML
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.ui.RoundedWrapper
import java.awt.Font
import java.awt.event.*
import javax.swing.JPanel
import javax.swing.text.JTextComponent

class SortingBasedTaskSpecificPanel(task: SortingBasedTask) : Wrapper() {
  private val emptyBorder = JBUI.Borders.empty(8, 12, 8, 6)
  private val roundedBorderSize = 1
  private val focusedRoundedBorderSize = 2

  private val arcSize = 8

  private val rowGapSize = 12
  private val columnGapSize = 8

  private val codeFont = Font.decode(StyleManager().codeFont)

  private val valueTextComponents = mutableListOf<JTextComponent>()

  init {
    val panel = panel {
      createShortcutTutorialHint()
        .customize(UnscaledGapsY(0, rowGapSize / 2))
      for (index in task.ordering.indices) {
        createOption(task, index)
          .layout(RowLayout.PARENT_GRID)
          .customize(UnscaledGapsY(0, rowGapSize))
      }
      align(Align.FILL)
    }.apply {
      isOpaque = false
      border = JBUI.Borders.empty(4, 0, 10, 10)
    }
    setContent(panel)
  }

  private fun Panel.createOption(task: SortingBasedTask, index: Int): Row = row {
    val optionPanel = createOptionPanel(task, index).apply {
      foreground = MatchingTaskUI.Value.foreground()
      background = MatchingTaskUI.Value.background()
      isFocusable = true
      addKeyListener(createValueKeyListener(task, index))
      addFocusListener(createValueFocusListener())
      addMouseListener(createMouseListener())
      recalcOptionPanelBorder()
    }

    valueTextComponents[index].addMouseListener(optionPanel.createMouseListener())

    if (task is MatchingTask) {
      val indexPanel = createIndexPanel(task, index).apply {
        foreground = MatchingTaskUI.Key.foreground()
        background = MatchingTaskUI.Key.background()
        addBorder(emptyBorder)
        addBorder(RoundedLineBorder(MatchingTaskUI.Key.background(), arcSize, roundedBorderSize))
      }

      cell(RoundedWrapper(indexPanel, arcSize))
        .widthGroup("IndexPanel")
        .align(AlignY.FILL)
        .customize(UnscaledGaps(0, 0, 0, columnGapSize))
    }

    cell(RoundedWrapper(optionPanel, arcSize))
      .align(Align.FILL)
      .resizableColumn()
  }

  private fun JPanel.recalcOptionPanelBorder() {
    border = emptyBorder
    if (!isFocusOwner) {
      addBorder(RoundedLineBorder(MatchingTaskUI.Value.borderColor(), arcSize, roundedBorderSize))
      addBorder(RoundedLineBorder(JBColor.background(), arcSize, focusedRoundedBorderSize - roundedBorderSize))
    } else {
      addBorder(RoundedLineBorder(UIUtil.getFocusedBorderColor(), arcSize, focusedRoundedBorderSize))
    }
  }

  private fun createIndexPanel(task: MatchingTask, index: Int): JPanel {
    return panel {
      row {
        text(task.captions[index])
          .align(AlignY.CENTER)
          .resizableColumn()
          .apply {
            component.font = Font(codeFont.name, codeFont.style, 13)
          }
        resizableRow()
      }
    }
  }

  private fun createOptionPanel(task: SortingBasedTask, index: Int): JPanel {
    return panel {
      row {
        val optionIndex = task.ordering[index]
        val textComponent = text(task.options[optionIndex])
          .align(AlignY.CENTER)
          .resizableColumn()
          .component.apply {
            font = Font(codeFont.name, codeFont.style, 13)
          }

        valueTextComponents += textComponent
        cell(createNavigationButtonsPanel(task, index))
          .align(AlignY.CENTER)
          .align(AlignX.RIGHT)
        resizableRow()
      }
    }
  }

  private fun createNavigationButtonsPanel(task: SortingBasedTask, index: Int): JPanel {
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
      EducationalCoreIcons.TaskToolWindow.MoveUp
    ) {
      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

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
      EducationalCoreIcons.TaskToolWindow.MoveDown
    ) {
      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

      override fun actionPerformed(e: AnActionEvent) = moveDown(task, index)

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = index + 1 < task.options.size
      }
    }
  }

  private fun JPanel.createValueKeyListener(task: SortingBasedTask, index: Int): KeyListener {
    return object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent?) {
        if (e == null) return
        when {
          (e.keyCode == KeyEvent.VK_DOWN) -> {
            if (index + 1 >= task.options.size) return
            if (e.isShiftDown) {
              moveDown(task, index)
            }
            transferFocus()
          }
          (e.keyCode == KeyEvent.VK_UP) -> {
            if (index <= 0) return
            if (e.isShiftDown) {
              moveUp(task, index)
            }
            transferFocusBackward()
          }
        }
      }
    }
  }

  private fun JPanel.createValueFocusListener(): FocusListener {
    return object : FocusAdapter() {
      override fun focusGained(e: FocusEvent?) {
        recalcOptionPanelBorder()
        repaint()
      }

      override fun focusLost(e: FocusEvent?) {
        recalcOptionPanelBorder()
        repaint()
      }
    }
  }

  private fun JPanel.createMouseListener(): MouseAdapter {
    return object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent?) {
        requestFocus()
      }
    }
  }

  private fun Panel.createShortcutTutorialHint(): Row = row {
    val xIcon = "<label class='textShortcut'>↑</label>"
    val yIcon = "<label class='textShortcut'>↓</label>"
    val attributes = "style='color: ${MatchingTaskUI.Key.foreground().asCssColor().value};'"

    comment(getSortingShortcutHTML(xIcon, yIcon, attributes))
  }
}