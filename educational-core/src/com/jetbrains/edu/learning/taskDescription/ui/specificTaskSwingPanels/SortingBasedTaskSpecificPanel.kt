package com.jetbrains.edu.learning.taskDescription.ui.specificTaskSwingPanels

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.Gaps
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import com.intellij.ui.dsl.gridLayout.VerticalGaps
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.ui.withBorder
import com.jetbrains.edu.learning.taskDescription.ui.withEmptyBorder
import javax.swing.JPanel
import kotlin.math.max

class SortingBasedTaskSpecificPanel(task: SortingBasedTask): SpecificTaskPanel() {
  private val emptyBorderSize = 12
  private val roundedBorderSize = 2

  private val arcSize = 10

  private val gapSize = 15

  private val optionPanelBorderColor = JBUI.CurrentTheme.Button.buttonColorEnd()
  private val indexPanelBackgroundColor = JBUI.CurrentTheme.ActionButton.hoverBackground()
  private val indexPanelBorderColor = indexPanelBackgroundColor

  private val model: Model

  init {
    model = Model(task)
    val panel = panel {
      for (index in task.ordering.indices) {
        createOption(task, index)
          .layout(RowLayout.PARENT_GRID)
          .customize(VerticalGaps(bottom = gapSize))
      }
      // BACKCOMPAT: 2022.2. Use align(Align.FILL)
      @Suppress("UnstableApiUsage", "DEPRECATION")
      horizontalAlign(HorizontalAlign.FILL)
      @Suppress("UnstableApiUsage", "DEPRECATION")
      verticalAlign(VerticalAlign.FILL)
    }.apply {
      isOpaque = false
      border = JBUI.Borders.empty(specificPanelInsets)
    }
    setContent(panel)
  }

  private fun Panel.createOption(task: SortingBasedTask, index: Int): Row = row {
    val indexPanel = createIndexPanel(task, index)
      ?.withEmptyBorder(emptyBorderSize)
      ?.withBorder(RoundedLineBorder(indexPanelBorderColor, arcSize, roundedBorderSize))

    val optionPanel = createOptionPanel(task, index)
      .withEmptyBorder(emptyBorderSize)
      .withBorder(RoundedLineBorder(optionPanelBorderColor, arcSize, roundedBorderSize))

    syncHeight(indexPanel, optionPanel)

    if (indexPanel != null) {
      indexPanel.background = indexPanelBackgroundColor
      cell(RoundedWrapper(indexPanel, arcSize))
        .widthGroup("IndexPanel")
        .customize(Gaps(right = gapSize))
    }

    // BACKCOMPAT: 2022.2. Use align(AlignX.FILL)
    @Suppress("UnstableApiUsage", "DEPRECATION")
    cell(RoundedWrapper(optionPanel, arcSize))
      .horizontalAlign(HorizontalAlign.FILL)
      .resizableColumn()
  }

  private fun syncHeight(panel1: JPanel?, panel2: JPanel?) {
    if (panel1 == null || panel2 == null) return
    val height1 = panel1.preferredSize.height
    val height2 = panel2.preferredSize.height
    val maxHeight = max(height1, height2)
    val dh1 = maxHeight - height1
    val dh2 = maxHeight - height2
    panel1.withBorder(JBUI.Borders.empty(dh1 / 2, 0, dh1 / 2 + dh1 % 2, 0))
    panel2.withBorder(JBUI.Borders.empty(dh2 / 2, 0, dh2 / 2 + dh2 % 2, 0))
  }

  private fun createIndexPanel(task: SortingBasedTask, index: Int): DialogPanel? {
    if (task !is MatchingTask) return null
    return panel {
      row {
        // BACKCOMPAT: 2022.2. Use align(AlignY.CENTER)
        @Suppress("UnstableApiUsage", "DEPRECATION")
        text(task.captions[index])
          .verticalAlign(VerticalAlign.CENTER)
      }
    }
  }

  private fun createOptionPanel(task: SortingBasedTask, index: Int): DialogPanel {
    return panel {
      row {
        val optionIndex = task.ordering[index]
        // BACKCOMPAT: 2022.2. Use align(AlignY.CENTER)
        @Suppress("UnstableApiUsage", "DEPRECATION")
        text(task.options[optionIndex])
          .bindText(model.getProperty(index))
          .verticalAlign(VerticalAlign.CENTER)
          .resizableColumn()


        // BACKCOMPAT: 2022.2. Use align(AlignY.CENTER) and align(AlignX.RIGHT)
        @Suppress("UnstableApiUsage", "DEPRECATION")
        panel { createNavigationsButtonsPanelContent(task, index) }
          // BACKCOMPAT: 2022.2. Use align(AlignY.CENTER)
          .verticalAlign(VerticalAlign.CENTER)
          .horizontalAlign(HorizontalAlign.RIGHT)
      }
      resizableColumn()
    }
  }

  private fun Panel.createNavigationsButtonsPanelContent(task: SortingBasedTask, index: Int) {
    row {
      actionButton(createUpAction(task, index))
    }
    row {
      actionButton(createDownAction(task, index))
    }
  }

  private fun createUpAction(task: SortingBasedTask, index: Int): DumbAwareAction {
    return object : DumbAwareAction(
      EduCoreBundle.lazyMessage("sorting.based.task.move.up"),
      EduCoreBundle.lazyMessage("sorting.based.task.move.up.description"),
      AllIcons.Actions.PreviousOccurence
    ) {
      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

      override fun actionPerformed(e: AnActionEvent) {
        task.moveOptionUp(index)
        model.swapValues(index, index - 1)
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = index > 0
      }
    }
  }

  private fun createDownAction(task: SortingBasedTask, index: Int): DumbAwareAction {
    return object : DumbAwareAction(
      EduCoreBundle.lazyMessage("sorting.based.task.move.down"),
      EduCoreBundle.lazyMessage("sorting.based.task.move.down.description"),
      AllIcons.Actions.NextOccurence
    ) {
      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

      override fun actionPerformed(e: AnActionEvent) {
        task.moveOptionDown(index)
        model.swapValues(index, index + 1)
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = index + 1 < task.options.size
      }
    }
  }

  private class Model(task: SortingBasedTask) {
    private val orderedOptions: Array<AtomicProperty<String>> = Array(task.ordering.size) {
      val optionIndex = task.ordering[it]
      AtomicProperty(task.options[optionIndex])
    }

    fun swapValues(i: Int, j: Int) {
      val s1 = orderedOptions[i].get()
      val s2 = orderedOptions[j].get()
      orderedOptions[i].set(s2)
      orderedOptions[j].set(s1)
    }

    fun getProperty(index: Int): ObservableMutableProperty<String> = orderedOptions[index]
  }
}