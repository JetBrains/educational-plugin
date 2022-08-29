package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.compatibility
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.CoursesDownloadingException
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.errors.ErrorState
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon
import javax.swing.JPanel

/**
 * Specifies information for the tab on [CoursesPanelWithTabs]
 */
abstract class CoursesPlatformProvider {
  abstract val name: String

  abstract val icon: Icon?

  abstract fun createPanel(scope: CoroutineScope, disposable: Disposable): CoursesPanel

  open fun joinAction(courseInfo: CourseInfo, courseMode: CourseMode, coursePanel: CoursePanel) {
    joinCourse(courseInfo, courseMode, coursePanel) { coursePanel.setError(it) }
  }

  suspend fun loadCourses(): List<CoursesGroup> {
    val courseGroups = try {
      doLoadCourses()
    }
    catch (e: CoursesDownloadingException) {
      throw e
    }
    catch (e: Exception) {
      Logger.getInstance(CoursesPlatformProvider::class.java).warn(e.message)
      return emptyList()
    }
    return courseGroups.mapNotNull { courseGroup ->
      val filteredCourses = courseGroup.courses.filter {
        val compatibility = it.compatibility
        compatibility == CourseCompatibility.Compatible || compatibility is CourseCompatibility.PluginsRequired || compatibility is CourseCompatibility.IncompatibleVersion
      }

      if (filteredCourses.isEmpty()) return@mapNotNull null
      courseGroup.courses = filteredCourses
      courseGroup
    }
  }

  protected abstract suspend fun doLoadCourses(): List<CoursesGroup>

  companion object {
    fun joinCourse(courseInfo: CourseInfo,
                   courseMode: CourseMode,
                   component: JPanel?,
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

          if (component != null) {
            val dialog = UIUtil.getParentOfType(DialogWrapperDialog::class.java, component)
            dialog?.dialogWrapper?.close(DialogWrapper.OK_EXIT_CODE)
          }
          course.courseMode = courseMode
          val projectGenerator = configurator.courseBuilder.getCourseProjectGenerator(course)
          val project = projectGenerator?.doCreateCourseProject(location, projectSettings)
          // null project means that user hasn't created course project at all.
          // For example, he/she may choose `Don't Open` option in `Trust and Open Project` dialog
          if (project != null) {
            CoursesStorage.getInstance().addCourse(course, location)
          }
        }
        catch (e: CourseCantBeStartedException) {
          errorHandler(e.error)
        }
      }
    }
  }
}
