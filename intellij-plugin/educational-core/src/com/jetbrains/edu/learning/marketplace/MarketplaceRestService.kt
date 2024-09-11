package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenCourseRequest
import io.netty.handler.codec.http.QueryStringDecoder

class MarketplaceRestService : BaseMarketplaceRestService(MARKETPLACE) {
  override fun getServiceName(): String = MarketplaceConnector.getInstance().serviceName

  override fun createMarketplaceOpenCourseRequest(urlDecoder: QueryStringDecoder): MarketplaceOpenCourseRequest? {
    val courseId = getIntParameter(COURSE_ID, urlDecoder)
    if (courseId == -1) return null

    val eduTaskId = getIntParameter(EDU_TASK_ID, urlDecoder)
    return MarketplaceOpenCourseRequest(courseId, eduTaskId)
  }

  companion object {
    const val COURSE_ID = "course_id"
  }
}