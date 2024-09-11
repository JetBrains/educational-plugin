package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.marketplace.BaseMarketplaceRestService
import com.jetbrains.edu.learning.marketplace.LTI
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenCourseRequest
import io.netty.handler.codec.http.QueryStringDecoder

class LTIRestService : BaseMarketplaceRestService(LTI) {
  override fun createMarketplaceOpenCourseRequest(urlDecoder: QueryStringDecoder): MarketplaceOpenCourseRequest? {
    val courseId = getIntParameter(COURSE_ID, urlDecoder)
    if (courseId == -1) return null
    val eduTaskId = getIntParameter(EDU_TASK_ID, urlDecoder)
    val launchId = getStringParameter(LAUNCH_ID, urlDecoder)
    val lmsDescription = getStringParameter(LMS_DESCRIPTION, urlDecoder)
    logger<LTIRestService>().info("LTI request with course=$courseId launchId=$launchId eduTaskId=$eduTaskId: $lmsDescription")

    return MarketplaceOpenCourseRequest(courseId, eduTaskId, LTISettingsDTO(launchId, lmsDescription))
  }

  override fun getServiceName(): String = "edu/lti"

  companion object {
    private const val LAUNCH_ID = "launch_id"
    private const val LMS_DESCRIPTION = "lms_description"
    private const val COURSE_ID = "marketplace_course_id"
  }
}