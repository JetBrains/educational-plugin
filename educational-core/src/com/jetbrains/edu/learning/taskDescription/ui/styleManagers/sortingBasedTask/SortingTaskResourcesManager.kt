package com.jetbrains.edu.learning.taskDescription.ui.styleManagers.sortingBasedTask

import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import kotlinx.css.*

class SortingTaskResourcesManager : SortingBasedTaskResourcesManager<SortingTask>() {
  override val templateName: String = "sortingTask.html"
  override val resources: Map<String, String>
    get() = mapOf("sorting_style" to stylesheet)

  override val stylesheet: String
    get() {
      return super.stylesheet + CSSBuilder().apply {
        "#sortingOptions" {
          paddingLeft = 8.px
        }
        "#keyValueGrid" {
          gridTemplateColumns = GridTemplateColumns(GridAutoRows.auto)
        }
        ".value" {
          gridColumn = GridColumn("1")
        }
      }.toString()
    }
}