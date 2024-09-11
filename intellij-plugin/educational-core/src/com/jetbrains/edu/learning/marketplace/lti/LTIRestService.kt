package com.jetbrains.edu.learning.marketplace.lti

import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.marketplace.BaseMarketplaceRestService
import com.jetbrains.edu.learning.marketplace.LTI
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenCourseRequest
import io.netty.handler.codec.http.QueryStringDecoder

class LTIRestService : BaseMarketplaceRestService(LTI) {
  override fun createMarketplaceOpenCourseRequest(urlDecoder: QueryStringDecoder): Result<MarketplaceOpenCourseRequest, String> {
    val courseId = getIntParameter(COURSE_ID, urlDecoder)
    if (courseId == -1) {
      return Err("LTI request has no course id.")
    }

    val eduTaskId = getIntParameter(EDU_TASK_ID, urlDecoder)
    if (eduTaskId == -1) {
      return Err("LTI request has no task id.")
    }

    val launchId = getStringParameter(LAUNCH_ID, urlDecoder)
    if (launchId == null) {
      return Err("LTI request has no launchId.")
    }

    val lmsDescription = getStringParameter(LMS_DESCRIPTION, urlDecoder)

    return Ok(MarketplaceOpenCourseRequest(courseId, eduTaskId, LTISettingsDTO(launchId, lmsDescription)))
  }

  override fun getServiceName(): String = "edu/lti"

  companion object {
    private const val LAUNCH_ID = "launch_id"
    private const val LMS_DESCRIPTION = "lms_description"
    private const val COURSE_ID = "marketplace_course_id"
  }
}