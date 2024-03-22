package com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.sortingBasedTask

import com.google.gson.Gson
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.newproject.ui.asCssColor
import com.jetbrains.edu.learning.taskToolWindow.ui.MatchingTaskUI
import kotlinx.css.*

class MatchingTaskResourcesManager : SortingBasedTaskResourcesManager<MatchingTask>() {
  override fun getCaptions(task: MatchingTask): String = Gson().toJson(task.captions)

  override fun taskSpecificStyles(): Map<String, String> = mapOf(wrapIntoStyleName(MatchingTask.MATCHING_TASK_TYPE) to stylesheet)

  override val stylesheet: String
    get() {
      return super.stylesheet + CSSBuilder().apply {
        "#options" {
          paddingLeft = 8.px
        }
        "#keyValueGrid" {
          columnGap = ColumnGap(8.px.value)
          gridTemplateColumns = GridTemplateColumns(
            GridAutoRows("fit-content(50%)"),
            GridAutoRows.auto
          )
        }
        ".key" {
          display = Display.table
          gridColumn = GridColumn("1")
          padding = "10px 12px 10px 12px"
          background = MatchingTaskUI.Key.background().asCssColor().value
          borderRadius = 4.px
        }
        ".keyLabel" {
          display = Display.tableCell
          verticalAlign = VerticalAlign.middle
        }
        ".value" {
          gridColumn = GridColumn("2")
        }
      }.toString()
    }
}