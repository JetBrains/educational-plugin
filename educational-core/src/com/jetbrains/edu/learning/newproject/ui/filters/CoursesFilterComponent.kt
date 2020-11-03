package com.jetbrains.edu.learning.newproject.ui.filters

import com.intellij.ui.FilterComponent
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import org.jetbrains.annotations.NonNls
import java.util.*
import kotlin.collections.HashSet

class CoursesFilterComponent(
  private val getCoursesGroups: () -> List<CoursesGroup>,
  private val updateModel: (List<CoursesGroup>) -> Unit
) : FilterComponent("Edu.NewCourse", 5, true) {

  init {
    textEditor.border = null
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
    val courseName = course.name.toLowerCase(Locale.getDefault())
    for (filterPart in filterParts) {
      if (courseName.contains(filterPart)) return true
      for (tag in course.tags) {
        if (tag.accept(filterPart)) {
          return true
        }
      }
      for (authorName in course.authorFullNames) {
        if (authorName.toLowerCase(Locale.getDefault()).contains(filterPart)) {
          return true
        }
      }
    }
    return false
  }

  private fun getFilterParts(@NonNls filter: String): Set<String> {
    return HashSet(listOf(*filter.toLowerCase().split(" ".toRegex()).toTypedArray()))
  }
}
