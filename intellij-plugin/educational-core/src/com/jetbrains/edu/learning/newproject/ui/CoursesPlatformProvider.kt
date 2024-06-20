package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.compatibility
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.CoursesDownloadingException
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
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

  open fun joinAction(courseInfo: CourseCreationInfo, courseMode: CourseMode, coursePanel: CoursePanel) {
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
      logger<CoursesPlatformProvider>().warn(e)
      return emptyList()
    }
    return courseGroups.mapNotNull { courseGroup ->
      courseGroup.courses
        .filter {
          when (it.compatibility) {
            is CourseCompatibility.Compatible,
            is CourseCompatibility.IncompatibleVersion,
            is CourseCompatibility.PluginsRequired -> true

            else -> false
          }
        }
        .takeIf { it.isNotEmpty() }
        ?.let { courseGroup.copy(courses = it) }
    }
  }

  protected abstract suspend fun doLoadCourses(): List<CoursesGroup>

  companion object {
    fun joinCourse(
      courseInfo: CourseCreationInfo,
      courseMode: CourseMode,
      component: JPanel?,
      errorHandler: (ErrorState) -> Unit
    ): Project? {
      val (course, location, projectSettings) = courseInfo

      // location is null for course preview dialog only
      if (location == null) {
        return null
      }

      val configurator = course.configurator
      // If `configurator != null` than `projectSettings` is always not null
      // because project settings are produced by configurator itself
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
          return project
        }
        catch (e: CourseCantBeStartedException) {
          errorHandler(e.error)
        }
      }
      return null
    }
  }
}
