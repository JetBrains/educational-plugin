package com.jetbrains.edu.learning.newproject.ui

import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.CoursesStorage
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import javax.swing.Icon
import javax.swing.JPanel

/**
 * Specifies information for the tab on [CoursesPanelWithTabs]
 */
abstract class CoursesPlatformProvider {
  abstract val name: String

  abstract val icon: Icon

  abstract val panel: CoursesPanel

  open fun joinAction(courseInfo: CourseInfo, courseMode: CourseMode, coursePanel: JPanel) {
    joinCourse(courseInfo, courseMode, coursePanel) { panel.setError(it) }
  }

  abstract suspend fun loadCourses(): List<Course>

  companion object {
    fun joinCourse(courseInfo: CourseInfo,
                   courseMode: CourseMode,
                   component: JPanel,
                   errorHandler: (ErrorState) -> Unit
    ) {
      val (course, getLocation, _) = courseInfo

      // location is null for course preview dialog only
      val location = getLocation()
      if (location == null) {
        return
      }

      CoursesStorage.getInstance().addCourse(course, location)

      val configurator = course.configurator
      // If `configurator != null` than `projectSettings` is always not null
      // because project settings are produced by configurator itself
      val projectSettings = courseInfo.projectSettings
      if (configurator != null && projectSettings != null) {
        try {
          configurator.beforeCourseStarted(course)

          val dialog = UIUtil.getParentOfType(JoinCourseDialogBase::class.java, component)
          dialog?.close()
          course.courseMode = courseMode.toString()
          val projectGenerator = configurator.courseBuilder.getCourseProjectGenerator(course)
          projectGenerator?.doCreateCourseProject(location, projectSettings)
        }
        catch (e: CourseCantBeStartedException) {
          errorHandler(e.error)
        }
      }
    }

  }
}

