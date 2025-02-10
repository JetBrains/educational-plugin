package com.jetbrains.edu.learning.newproject.ui.filters

import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.Dimension

class HumanLanguageFilterDropdown(filterCourses: () -> Unit) : FilterDropdown(emptySet(), filterCourses) {
  override val popupSize: Dimension = JBUI.size(180, 150)
  override var selectedItems: Set<String> = emptySet()

  init {
    text = title()
  }

  private fun title(): String =
    if (selectedItems.isEmpty()) EduCoreBundle.message("course.dialog.filter.languages")
    else EduCoreBundle.message("course.dialog.filter.all.languages")

  override fun defaultTitle(): String = EduCoreBundle.message("course.dialog.filter.languages")

  override fun isAccepted(course: Course): Boolean = course.humanLanguage in selectedItems

  override fun resetSelection() {
    selectedItems = emptySet()
    text = title()
  }

  override fun updateItems(items: Set<String>) {
    super.updateItems(items)
    selectedItems = selectedItems.union(items)
    text = title()
  }
}