package com.jetbrains.edu.learning.newproject.ui.filters

import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.Dimension

class HumanLanguageFilterDropdown(filterCourses: () -> Unit) : FilterDropdown(emptySet(), filterCourses) {
  override val popupSize: Dimension = JBUI.size(180, 150)
  override var selectedItems: Set<String> = emptySet()
  override val defaultTitle: String
    get() = EduCoreBundle.message("course.dialog.filter.languages")

  override fun isAccepted(course: Course): Boolean = course.humanLanguage in selectedItems

  override fun resetSelection() {
    selectedItems = emptySet()
    text = defaultTitle
  }

  override fun updateItems(items: Set<String>) {
    super.updateItems(items)
    selectedItems = selectedItems.union(items)
    if (allItems.size == selectedItems.size) {
      text = allSelectedTitle()
    } else {
      text = defaultTitle
    }
  }
}