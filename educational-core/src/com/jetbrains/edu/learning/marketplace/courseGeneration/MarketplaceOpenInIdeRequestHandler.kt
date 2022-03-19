package com.jetbrains.edu.learning.marketplace.courseGeneration

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.compatibility.CourseCompatibility.Companion.validateLanguage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequestHandler
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.checkForUpdates
import com.jetbrains.edu.learning.marketplace.updateFeaturedStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle

object MarketplaceOpenInIdeRequestHandler : OpenInIdeRequestHandler<MarketplaceOpenCourseRequest>() {
  override val courseLoadingProcessTitle: String get() = EduCoreBundle.message("action.get.course.loading")

  override fun openInExistingProject(request: MarketplaceOpenCourseRequest, findProject: ((Course) -> Boolean) -> Pair<Project, Course>?): Boolean {
    val (project, course) = findProject { it.isMarketplace && it.id == request.courseId } ?: return false
    val marketplaceCourse = course as? EduCourse ?: return false
    synchronizeCourse(project, marketplaceCourse)
    return true
  }

  override fun getCourse(request: MarketplaceOpenCourseRequest, indicator: ProgressIndicator): Result<Course, String> {
    val course = MarketplaceConnector.getInstance().searchCourse(request.courseId)
    if (course == null) {
      return Err(EduCoreBundle.message("marketplace.course.loading.failed"))
    }

    if (course.environment == EduNames.ANDROID && !EduUtils.isAndroidStudio()) {
      return Err(EduCoreBundle.message("rest.service.android.not.supported"))
    }

    course.validateLanguage().onError { return Err(it) }
    course.updateFeaturedStatus()

    return Ok(course)
  }

  private fun synchronizeCourse(project: Project, course: EduCourse) {
    if (isUnitTestMode) {
      return
    }
    course.checkForUpdates(project, true) {}
  }
}