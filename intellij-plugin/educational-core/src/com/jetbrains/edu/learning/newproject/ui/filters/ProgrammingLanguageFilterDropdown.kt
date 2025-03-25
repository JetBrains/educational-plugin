package com.jetbrains.edu.learning.newproject.ui.filters

import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.supportedTechnologies
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.Dimension

class ProgrammingLanguageFilterDropdown(
  supportedLanguages: Set<String>,
  filterCourses: () -> Unit
) : FilterDropdown(supportedLanguages, filterCourses) {
  override val popupSize: Dimension = JBUI.size(210, 170)
  override var selectedItems: Set<String> = supportedLanguages
  override val defaultTitle: String
    get() = EduCoreBundle.message("course.dialog.filter.programming.languages")

  init {
    text = defaultTitle
  }

  override fun updateItems(items: Set<String>) {
    val allSelected = allItems.size == selectedItems.size
    super.updateItems(items)
    if (allSelected) {
      selectedItems = items
      text = allSelectedTitle()
    }
  }

  override fun resetSelection() {
    selectedItems = allItems
    text = allSelectedTitle()
  }

  override fun isAccepted(course: Course): Boolean = course.supportedTechnologies.intersect(selectedItems).isNotEmpty()
}