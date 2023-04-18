package com.jetbrains.edu.learning.taskDescription.ui.styleManagers.sortingBasedTask

import com.google.gson.Gson
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.newproject.ui.asCssColor
import com.jetbrains.edu.learning.taskDescription.ui.MatchingTaskUI
import kotlinx.css.*

class MatchingTaskResourcesManager : SortingBasedTaskResourcesManager<MatchingTask>() {
  override val templateName: String = "matchingTask.html"

  override val resources: Map<String, String>
    get() = mapOf("matching_style" to stylesheet)

  override fun getTextResources(task: MatchingTask): Map<String, String> {
    return super.getTextResources(task) + mapOf("captions" to Gson().toJson(task.captions))
  }

  override val stylesheet: String
    get() {
      return super.stylesheet + CSSBuilder().apply {
        "#matchingOptions" {
          display = Display.grid
          paddingLeft = 8.px
          gridTemplateColumns = GridTemplateColumns(
            GridAutoRows.maxContent,
            GridAutoRows.auto
          )
          justifyContent = JustifyContent.stretch
          rowGap = RowGap(12.px.value)
          columnGap = ColumnGap(8.px.value)
          alignItems = Align.stretch
        }
        ".key" {
          display = Display.flex
          gridColumn = GridColumn("1")
          padding = "10px 12px 10px 12px"
          background = MatchingTaskUI.Key.background().asCssColor().value
          borderRadius = 4.px
          alignItems = Align.center
        }
        ".value" {
          gridColumn = GridColumn("2")
        }
      }.toString()
    }
}