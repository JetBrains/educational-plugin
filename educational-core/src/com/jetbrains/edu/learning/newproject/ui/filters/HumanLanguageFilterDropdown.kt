package com.jetbrains.edu.learning.newproject.ui.filters

import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.Dimension
import java.util.*

class HumanLanguageFilterDropdown(humanLanguages: Set<String>, filterCourses: () -> Unit) : FilterDropdown(humanLanguages, filterCourses) {
  override val popupSize: Dimension = JBUI.size(180, 150)
  override var selectedItems: Set<String> = languagesFromLocale()

  init {
    text = languagesFromLocale().joinToString(limit = 2)
  }

  override fun defaultTitle(): String = EduCoreBundle.message("course.dialog.filter.languages")

  override fun isAccepted(course: Course): Boolean = course.humanLanguage in selectedItems

  private fun languagesFromLocale() = setOf(Locale.getDefault().displayLanguage)
}