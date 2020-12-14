// BACKCOMPAT: 2019.3
@file:Suppress("DEPRECATION")

package com.jetbrains.edu.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

abstract class LoginWidgetProvider : StatusBarWidgetProvider {
  override fun getWidget(project: Project): StatusBarWidget? {
    if (EduUtils.isEduProject(project)) {
      val baseDir = project.courseDir
      val course = StudyTaskManager.getInstance(project).course

      val widgetAvailable = if (course != null) {
        isWidgetAvailable(course)
      }
      else {
        val courseType = baseDir.getUserData(CourseProjectGenerator.COURSE_TYPE_TO_CREATE) ?: throw IllegalStateException(
          "Both CourseType and Course should not be null")
        val languageId = baseDir.getUserData(CourseProjectGenerator.COURSE_LANGUAGE_ID_TO_CREATE)
        val courseMode = baseDir.getUserData(CourseProjectGenerator.COURSE_MODE_TO_CREATE)
        isWidgetAvailable(courseType, languageId, courseMode)
      }
      if (widgetAvailable) {
        return createLoginWidget(project)
      }
    }
    return null
  }

  override fun getAnchor(): String = StatusBar.Anchors.before(StatusBar.StandardWidgets.POSITION_PANEL)

  abstract fun isWidgetAvailable(courseType: String, languageId: String?, courseMode: String?): Boolean

  abstract fun isWidgetAvailable(course: Course): Boolean

  abstract fun createLoginWidget(project: Project): LoginWidget<out OAuthAccount<out Any>>
}