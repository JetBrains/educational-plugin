package com.jetbrains.edu.learning.marketplace.courseGeneration

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.CourseValidationResult
import com.jetbrains.edu.learning.courseFormat.ext.ValidationErrorMessage
import com.jetbrains.edu.learning.courseFormat.ext.validateLanguage
import com.jetbrains.edu.learning.courseGeneration.OpenInIdeRequestHandler
import com.jetbrains.edu.learning.marketplace.MarketplaceSolutionLoader
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.checkForUpdates
import com.jetbrains.edu.learning.marketplace.lti.LTISettingsManager
import com.jetbrains.edu.learning.marketplace.updateFeaturedStatus
import com.jetbrains.edu.learning.messages.EduCoreBundle

object MarketplaceOpenInIdeRequestHandler : OpenInIdeRequestHandler<MarketplaceOpenCourseRequest>() {
  override val courseLoadingProcessTitle: String get() = EduCoreBundle.message("action.get.course.loading")

  override fun openInExistingProject(
    request: MarketplaceOpenCourseRequest,
    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?
  ): Project? {
    val (project, course) = findProject { it.isMarketplace && it.id == request.courseId } ?: return null
    val marketplaceCourse = course as? EduCourse ?: return null
    synchronizeCourse(project, marketplaceCourse)
    return project
  }

  override fun getCourse(request: MarketplaceOpenCourseRequest, indicator: ProgressIndicator): Result<Course, CourseValidationResult> {
    val course = MarketplaceConnector.getInstance().searchCourse(request.courseId)
    if (course == null) {
      return Err(ValidationErrorMessage(EduCoreBundle.message("marketplace.course.loading.failed")))
    }

    if (course.environment == EduNames.ANDROID && !EduUtilsKt.isAndroidStudio()) {
      return Err(ValidationErrorMessage(EduCoreBundle.message("rest.service.android.not.supported")))
    }

    course.validateLanguage().onError { return Err(it) }
    course.updateFeaturedStatus()

    return Ok(course)
  }

  override fun afterProjectOpened(request: MarketplaceOpenCourseRequest, project: Project) {
    openStudyItem(request.studyItemId, project)

    val ltiSettings = request.ltiSettingsDTO
    if (ltiSettings != null) {
      val settingsState = LTISettingsManager.instance(project).state
      settingsState.launchId = ltiSettings.launchId
      settingsState.lmsDescription = ltiSettings.lmsDescription
      settingsState.onlineService = ltiSettings.onlineService
    }
  }

  private fun synchronizeCourse(project: Project, course: EduCourse) {
    if (isUnitTestMode) {
      return
    }
    course.checkForUpdates(project, true) {}

    MarketplaceSolutionLoader.getInstance(project).loadSolutionsInBackground()
  }
}