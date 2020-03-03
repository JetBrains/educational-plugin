package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ide.plugins.newui.EmptyCaret
import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course

abstract class CourseHtmlPanel : HtmlPanel() {
  protected var course: Course? = null

  init {
    background = UIUtil.getEditorPaneBackground()
    isFocusable = false
    border = null
    caret = EmptyCaret.INSTANCE
  }

  fun bind(course: Course) {
    this.course = course
    update()
  }
}