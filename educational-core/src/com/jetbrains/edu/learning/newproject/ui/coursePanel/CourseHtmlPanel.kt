package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ide.plugins.newui.EmptyCaret
import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle

abstract class CourseHtmlPanel : HtmlPanel() {
  protected var course: Course? = null

  init {
    background = UIUtil.getEditorPaneBackground()
    isFocusable = false
    border = null
    caret = EmptyCaret.INSTANCE

    // set some text to force JEditorPane calculate its height properly
    text = EduCoreBundle.message("course.dialog.no.course.selected")
  }

  fun bind(course: Course) {
    this.course = course
    update()
  }
}