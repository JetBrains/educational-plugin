package com.jetbrains.edu.learning.marketplace.courseGeneration

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.CourseValidationResult
import com.jetbrains.edu.learning.courseFormat.ext.ValidationErrorMessage
import com.jetbrains.edu.learning.courseFormat.ext.validateLanguage
import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequestHandler
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.updateFeaturedStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle

object MarketplaceOpenLtiLinkCourseRequestHandler : OpenInIdeRequestHandler<MarketplaceOpenLtiLinkCourseRequest>() {
  override val courseLoadingProcessTitle: String get() = EduCoreBundle.message("action.get.course.loading")

  override fun openInExistingProject(
    request: MarketplaceOpenLtiLinkCourseRequest,
    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?
  ): Boolean = false

  override fun getCourse(request: MarketplaceOpenLtiLinkCourseRequest, indicator: ProgressIndicator): Result<Course, CourseValidationResult> {
    val course = MarketplaceConnector.getInstance().searchCourse(request.courseId)
    if (course == null) {
      return Err(ValidationErrorMessage(EduCoreBundle.message("marketplace.course.loading.failed")))
    }

    if (course.environment == EduNames.ANDROID && !EduUtilsKt.isAndroidStudio()) {
      return Err(ValidationErrorMessage(EduCoreBundle.message("rest.service.android.not.supported")))
    }

    course.validateLanguage().onError { return Err(it) }
    course.updateFeaturedStatus()

    course.selectedTaskId = request.taskEduId
    course.ltiLaunchId = request.launchId

    return Ok(course)
  }
}