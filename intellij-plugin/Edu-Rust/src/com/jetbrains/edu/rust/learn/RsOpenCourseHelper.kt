package com.jetbrains.edu.rust.learn

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettingsPanel
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.statistics.metadata.EntryPointMetadataProcessor
import com.jetbrains.edu.rust.RsConfigurator
import com.jetbrains.edu.rust.RsProjectSettings
import com.jetbrains.edu.rust.messages.EduRustBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rust.cargo.toolchain.RsToolchainBase
import java.nio.file.Path
import kotlin.io.path.absolutePathString

// BACKCOMPAT: 2025.1. Merge it into RsOpenCourseHandler
object RsOpenCourseHelper {

  private const val RUST_ROVER_BANNER = "rustrover_banner"

  private val LOG = logger<RsOpenCourseHelper>()

  suspend fun openCourse(
    courseId: Int,
    toolchain: RsToolchainBase,
    projectLocation: Path?
  ) {
    val location = searchExistingCourseLocation(courseId)
    if (location != null) {
      withContext(Dispatchers.EDT) {
        val project = ProjectUtil.openProject(location, null, true)
        ProjectUtil.focusProjectWindow(project, true)
      }
    }
    else {
      val course = MarketplaceConnector.getInstance().searchCourse(courseId)
      if (course == null) {
        LOG.warn("Failed to load https://plugins.jetbrains.com/plugin/$courseId course")
        EduNotificationManager.showErrorNotification(
          title = EduRustBundle.message("course.creation.failed.notification.title"),
          content = EduRustBundle.message("course.creation.no.course.found.notification.content")
        )
      }
      else {
        withContext(Dispatchers.EDT) {
          blockingContext {
            course.createProject(toolchain, projectLocation)
          }
        }
      }
    }
  }

  // TODO: unify with `com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider.joinCourse`
  @RequiresBlockingContext
  private fun Course.createProject(toolchain: RsToolchainBase, projectLocation: Path?) {
    val configurator = course.configurator as? RsConfigurator ?: return
    val projectSettings = RsProjectSettings(toolchain)

    val location = course.getProjectLocation(projectLocation)

    try {
      configurator.beforeCourseStarted(course)

      val projectGenerator = configurator.courseBuilder.getCourseProjectGenerator(course)
      val project = projectGenerator?.doCreateCourseProject(
        location = location,
        projectSettings = projectSettings,
        openCourseParams = mapOf(EntryPointMetadataProcessor.ENTRY_POINT to RUST_ROVER_BANNER)
      )
      if (project != null) {
        CoursesStorage.getInstance().addCourse(course, location)
      }
      else {
        LOG.warn("Failed to create a project for course $name at $location")
        EduNotificationManager.showErrorNotification(
          title = EduRustBundle.message("course.creation.failed.notification.title"),
          content = EduRustBundle.message("course.creation.project.creation.failed.notification.content")
        )
      }
      return
    }
    catch (e: CourseCantBeStartedException) {
      LOG.warn(e)
      EduNotificationManager.showErrorNotification(
        title = EduRustBundle.message("course.creation.failed.notification.title"),
        content = EduRustBundle.message("course.creation.project.creation.failed.notification.content")
      )
    }
  }

  private fun Course.getProjectLocation(projectLocation: Path?): String {
    if (projectLocation != null) return projectLocation.absolutePathString()

    return CourseSettingsPanel.nameToLocation(this)
  }

  fun isAlreadyStartedCourse(courseId: Int): Boolean {
    return searchExistingCourseLocation(courseId) != null
  }

  private fun searchExistingCourseLocation(courseId: Int): String? {
    val location = CoursesStorage.getInstance().getAllCourses().find { it.id == courseId }?.location ?: return null
    return if (FileUtil.exists(location)) location else null
  }
}
