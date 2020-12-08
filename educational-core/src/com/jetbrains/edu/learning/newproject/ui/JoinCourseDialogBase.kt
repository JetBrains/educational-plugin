package com.jetbrains.edu.learning.newproject.ui

import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import javax.swing.JComponent

open class JoinCourseDialogBase(private val course: Course, settings: CourseDisplaySettings) : OpenCourseDialogBase() {
  private val coursePanel: CoursePanel = CoursePanel(isLocationFieldNeeded = true) { _, _, panel ->
    CoursesPlatformProvider.joinCourse(CourseInfo(course, { locationString }, { languageSettings }), CourseMode.STUDY, panel) {
      panel.setError(it)
    }
  }

  init {
    title = course.name
    coursePanel.bindCourse(course, settings)
    coursePanel.preferredSize = JBUI.size(500, 530)
  }

  override val courseInfo: CourseInfo
    get() = CourseInfo(course, { coursePanel.locationString }, { coursePanel.languageSettings })

  override fun createCenterPanel(): JComponent = coursePanel

  // '!!' is safe here because `myCoursePanel` has location field
  val locationString: String get() = coursePanel.locationString!!

  val languageSettings: LanguageSettings<*>? get() = coursePanel.languageSettings
}
