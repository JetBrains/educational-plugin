package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetProvider
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class HyperskillWidgetProvider : StatusBarWidgetProvider {
  override fun getWidget(project: Project): StatusBarWidget? {
    if (EduUtils.isStudyProject(project)) {
      val baseDir = project.baseDir
      val courseType = baseDir.getUserData(CourseProjectGenerator.COURSE_TYPE_TO_CREATE)
      if (courseType == HYPERSKILL || StudyTaskManager.getInstance(project).course is HyperskillCourse) {
        return HyperskillWidget()
      }
    }
    return null
  }

  override fun getAnchor(): String  = StatusBar.Anchors.before(StatusBar.StandardWidgets.POSITION_PANEL)
}
