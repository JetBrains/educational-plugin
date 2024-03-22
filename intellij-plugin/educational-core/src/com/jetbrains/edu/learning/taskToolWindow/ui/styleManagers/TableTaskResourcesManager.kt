package com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers

import com.google.gson.Gson
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.JavaUILibrary
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.asCssColor
import com.jetbrains.edu.learning.stepik.hyperskill.stepLink
import com.jetbrains.edu.learning.taskToolWindow.ui.*
import kotlinx.css.*
import kotlinx.css.properties.*

class TableTaskResourcesManager: TaskResourcesManager<TableTask> {
  private val templateName = "tableTask.html"

  override fun getText(task: TableTask): String {
    return if (task.rows.isNotEmpty() && task.columns.isNotEmpty()) {
      GeneratorUtils.getInternalTemplateText(templateName, getTextResources(task))
    }
    else {
      EduCoreBundle.message("ui.task.specific.panel", stepLink(task.id), EduNames.JBA)
    }
  }

  private fun getTextResources(task: TableTask): Map<String, Any> {
    return mapOf(
      "rows" to Gson().toJson(task.rows),
      "columns" to Gson().toJson(task.columns),
      "selected" to task.selected.map { it.toList()} .toList(),
      "is_checkbox" to task.isMultipleChoice,
      "table_task_style" to stylesheet
    )
  }

  private val stylesheet: String
    get() {
      val styleManager = StyleManager()
      return CSSBuilder().apply {
        "#table" {
          fontFamily = styleManager.bodyFont
          fontSize = if (JavaUILibrary.isJCEF()) styleManager.bodyFontSize.px else styleManager.bodyFontSize.pt
          lineHeight = (1.3).em.lh
          color = styleManager.bodyColor
          textAlign = TextAlign.left
          paddingLeft = 8.px
          borderCollapse = BorderCollapse.collapse
          width = LinearDimension("100%")
        }

        "#table td" {
          border = "1px solid"
          borderColor = getBorderColor()
          padding = "8px"
          verticalAlign = VerticalAlign.middle
          wordWrap = WordWrap.breakWord
        }

        ".row_header" {
          textAlign = TextAlign.left
          color = JBColor.namedColor("Table.foreground").asCssColor()
        }

        "#table td input" {
          display = Display.inlineBlock
        }

        ".input_cell, .header_cell" {
          textAlign = TextAlign.center
          color = JBColor.namedColor("Table.foreground").asCssColor()
        }

        "#table tr:hover" {
          backgroundColor = getHoverColor()
        }
      }.toString()
        .plus(getRadioButtonCSS())
    }

  private fun getBorderColor(): Color {
    return JBUI.CurrentTheme.Table.Hover.background(false).asCssColor()
  }

  private fun getHoverColor(): Color {
    return JBUI.CurrentTheme.Table.Hover.background(true).asCssColor()
  }
}