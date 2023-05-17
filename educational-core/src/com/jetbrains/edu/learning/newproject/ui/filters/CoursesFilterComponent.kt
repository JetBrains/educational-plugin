package com.jetbrains.edu.learning.newproject.ui.filters

import com.intellij.ide.plugins.newui.ListPluginComponent
import com.intellij.ui.FilterComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StatusText
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.tags
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import org.jetbrains.annotations.NonNls
import java.util.*


class CoursesFilterComponent(
  emptySearchText: String,
  private val getCoursesGroups: () -> List<CoursesGroup>,
  private val updateModel: (List<CoursesGroup>) -> Unit
) : FilterComponent("Edu.NewCourse", 5, true) {

  init {
    removeBorder()
    val emptyText: StatusText = (textEditor as JBTextField).emptyText
    emptyText.appendText(emptySearchText, SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, ListPluginComponent.GRAY_COLOR))
  }

  private fun removeBorder() {
    textEditor.border = JBUI.Borders.empty()
  }

  fun resetSearchField() {
    textEditor.text = ""
  }

  override fun filter() {
    val filteredCoursesGroups = getCoursesGroups().map { coursesGroup ->
      CoursesGroup(coursesGroup.name, coursesGroup.courses.filter { accept(filter, it) })
    }
    updateModel(filteredCoursesGroups)
  }

  private fun accept(@NonNls filter: String, course: Course): Boolean {
    if (filter.isEmpty()) {
      return true
    }
    val filterParts = getFilterParts(filter)
    val courseName = course.name.lowercase(Locale.getDefault())
    for (filterPart in filterParts) {
      if (courseName.contains(filterPart)) return true
      for (tag in course.tags) {
        if (tag.accept(filterPart)) {
          return true
        }
      }
      for (authorName in course.authorFullNames) {
        if (authorName.lowercase(Locale.getDefault()).contains(filterPart)) {
          return true
        }
      }
    }
    return false
  }

  private fun getFilterParts(@NonNls filter: String): Set<String> {
    return HashSet(listOf(*filter.lowercase().split(" ".toRegex()).toTypedArray()))
  }
}
