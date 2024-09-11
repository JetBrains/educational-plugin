package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.Ok
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenCourseRequest
import io.netty.handler.codec.http.QueryStringDecoder

class MarketplaceRestService : BaseMarketplaceRestService(MARKETPLACE) {
  override fun getServiceName(): String = MarketplaceConnector.getInstance().serviceName

  override fun createMarketplaceOpenCourseRequest(urlDecoder: QueryStringDecoder): Result<MarketplaceOpenCourseRequest, String> {
    val courseId = getIntParameter(COURSE_ID, urlDecoder)
    if (courseId == -1) return Err("No course id specified")

    val eduTaskId = getIntParameter(EDU_TASK_ID, urlDecoder)
    return Ok(MarketplaceOpenCourseRequest(courseId, eduTaskId))
  }

  companion object {
    const val COURSE_ID = "course_id"
  }
}