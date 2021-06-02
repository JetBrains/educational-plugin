package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.jetbrains.edu.learning.LoginWidgetFactory
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.messages.EduPythonBundle

class PyCheckiOWidgetFactory : LoginWidgetFactory() {
  override val widgetId: String = "widget.pyCheckiO"

  override fun isWidgetAvailable(course: Course) = course is CheckiOCourse && course.name == CheckiONames.PY_CHECKIO

  override fun getDisplayName(): String = EduPythonBundle.message("checkio.widget.title")

  override fun createWidget(project: Project): StatusBarWidget = PyCheckiOWidget(project)
}