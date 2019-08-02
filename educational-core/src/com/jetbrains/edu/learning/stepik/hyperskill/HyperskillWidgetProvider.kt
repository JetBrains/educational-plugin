package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.LoginWidgetProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillWidgetProvider : LoginWidgetProvider() {

  override fun createLoginWidget(project: Project) = HyperskillWidget(project)

  override fun isWidgetAvailable(courseType: String, languageId: String?) = courseType == HYPERSKILL

  override fun isWidgetAvailable(course: Course) = course is HyperskillCourse
}
