package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.jetbrains.edu.learning.LoginWidgetFactory
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle

class StepikWidgetFactory : LoginWidgetFactory() {
  override val widgetId: String = "widget.stepik"

  override fun isWidgetAvailable(course: Course) = course is EduCourse && !course.isMarketplace && course.isStudy

  override fun getDisplayName(): String = EduCoreBundle.message("stepik.widget.title")

  override fun createWidget(project: Project): StatusBarWidget = StepikWidget(project)
}
