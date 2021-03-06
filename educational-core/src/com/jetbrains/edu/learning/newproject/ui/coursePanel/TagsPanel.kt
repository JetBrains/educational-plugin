package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course


private const val TAGS_TOP_OFFSET = 15
private const val HORIZONTAL_OFFSET = 8

class TagsPanel : NonOpaquePanel(HorizontalLayout(HORIZONTAL_OFFSET)), CourseSelectionListener {

  init {
    border = JBUI.Borders.empty(TAGS_TOP_OFFSET, HORIZONTAL_MARGIN, 0, 0)
  }

  private fun addTags(course: Course) {
    removeAll()
    for (tag in course.tags) {
      add(tag.createComponent())
    }
  }

  fun update(course: Course, settings: CourseDisplaySettings) {
    isVisible = settings.showTagsPanel
    if (settings.showTagsPanel) {
      addTags(course)
    }
  }

  override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
    isVisible = courseDisplaySettings.showTagsPanel
    if (courseDisplaySettings.showTagsPanel) {
      addTags(courseInfo.course)
    }
  }
}