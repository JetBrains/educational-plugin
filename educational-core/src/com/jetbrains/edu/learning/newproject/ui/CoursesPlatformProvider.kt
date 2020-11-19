package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon
import javax.swing.JPanel

/**
 * Specifies information for the tab on [CoursesPanelWithTabs]
 */
abstract class CoursesPlatformProvider {
  abstract val name: String

  abstract val icon: Icon?

  abstract fun createPanel(scope: CoroutineScope): CoursesPanel

  open fun joinAction(courseInfo: CourseInfo, courseMode: CourseMode, coursePanel: CoursePanel) {
    joinCourse(courseInfo, courseMode, coursePanel) { coursePanel.setError(it) }
  }

  abstract suspend fun loadCourses(): List<CoursesGroup>

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

      val configurator = course.configurator
      // If `configurator != null` than `projectSettings` is always not null
      // because project settings are produced by configurator itself
      val projectSettings = courseInfo.projectSettings
      if (configurator != null && projectSettings != null) {
        try {
          configurator.beforeCourseStarted(course)

          val dialog = UIUtil.getParentOfType(DialogWrapperDialog::class.java, component)
          dialog?.dialogWrapper?.close(DialogWrapper.OK_EXIT_CODE)
          course.courseMode = courseMode.toString()
          val projectGenerator = configurator.courseBuilder.getCourseProjectGenerator(course)
          projectGenerator?.doCreateCourseProject(location, projectSettings)
          CoursesStorage.getInstance().addCourse(course, location)
        }
        catch (e: CourseCantBeStartedException) {
          errorHandler(e.error)
        }
      }
    }

  }
}
