package com.jetbrains.edu.learning.stepik.hyperskill.widget

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.jetbrains.edu.learning.LoginWidgetFactory
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillWidgetFactory : LoginWidgetFactory() {
  override val widgetId: String = "widget.hyperskill"

  override fun isWidgetAvailable(course: Course) = course is HyperskillCourse

  override fun getDisplayName(): String = EduCoreBundle.message("hyperskill.widget.title")

  override fun createWidget(project: Project): StatusBarWidget = HyperskillWidget(project)
}
