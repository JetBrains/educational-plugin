package com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

open class HyperskillProjectManager {
  open fun newProject(course: HyperskillCourse) {
    JoinCourseDialog(course, CourseDisplaySettings(showTagsPanel = false, showInstructorField = false)).show()
  }

  companion object {
    @JvmStatic
    fun getInstance(): HyperskillProjectManager = service()
  }
}