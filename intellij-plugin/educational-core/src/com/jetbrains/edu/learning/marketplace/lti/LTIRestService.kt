package com.jetbrains.edu.learning.marketplace.lti

import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.marketplace.BaseMarketplaceRestService
import com.jetbrains.edu.learning.marketplace.LTI
import io.netty.handler.codec.http.QueryStringDecoder

class LTIRestService : BaseMarketplaceRestService<LTIOpenCourseRequest>(LTI) {
  override fun createMarketplaceOpenCourseRequest(urlDecoder: QueryStringDecoder): Result<LTIOpenCourseRequest, String> {
    val courseId = getIntParameter(COURSE_ID, urlDecoder)
    if (courseId == -1) {
      return Err("LTI request has no course id.")
    }

    var studyItemId = getIntParameter(STUDY_ITEM_ID, urlDecoder)
    if (studyItemId == -1) {
      // in previous versions of LTI we had `task_id` instead of `study_item_id`
      studyItemId = getIntParameter(EDU_TASK_ID, urlDecoder)

      if (studyItemId == -1) {
        return Err("The LTI request contains neither a study item ID nor a task ID.")
      }
    }

    val launchId = getStringParameter(LAUNCH_ID, urlDecoder)
    if (launchId == null) {
      return Err("LTI request has no launchId.")
    }

    val lmsDescription = getStringParameter(LMS_DESCRIPTION, urlDecoder)
    val onlineService = LTIOnlineService.detect(urlDecoder)

    val courseraCourse = getStringParameter(COURSERA_COURSE, urlDecoder)

    return Ok(LTIOpenCourseRequest(courseId, studyItemId, LTISettingsDTO(
      launchId,
      lmsDescription,
      onlineService,
      courseraCourse?.courseraCourseNameToLink()
    )))
  }

  override fun getServiceName(): String = "edu/lti"

  companion object {
    private const val LAUNCH_ID = "launch_id"
    private const val LMS_DESCRIPTION = "lms_description"
    private const val COURSE_ID = "marketplace_course_id"
    private const val COURSERA_COURSE = "coursera_course"
  }
}