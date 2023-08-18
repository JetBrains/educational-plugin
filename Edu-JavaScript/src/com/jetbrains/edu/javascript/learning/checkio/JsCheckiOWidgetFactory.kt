package com.jetbrains.edu.javascript.learning.checkio

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.jetbrains.edu.javascript.learning.messages.EduJavaScriptBundle
import com.jetbrains.edu.learning.LoginWidgetFactory
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.Course

class JsCheckiOWidgetFactory : LoginWidgetFactory() {
  override val widgetId: String = "widget.jsCheckiO"

  override fun isWidgetAvailable(course: Course) = course is CheckiOCourse && course.name == CheckiONames.JS_CHECKIO

  override fun getDisplayName(): String = EduJavaScriptBundle.message("checkio.widget.title")

  override fun createWidget(project: Project): StatusBarWidget = JsCheckiOWidget(project)
}