package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.marketplace.BaseMarketplaceRestService
import com.jetbrains.edu.learning.marketplace.LTI
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder

class LTIRestService : BaseMarketplaceRestService(LTI) {
  override val courseIdParamName: String = "marketplace_course_id"

  override fun execute(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val courseId = getIntParameter(courseIdParamName, urlDecoder)
    val launchId = getStringParameter(LAUNCH_ID, urlDecoder)
    val eduTaskId = getStringParameter(EDU_TASK_ID, urlDecoder)
    val lmsDescription = getStringParameter(LMS_DESCRIPTION, urlDecoder)
    logger<LTIRestService>().info("LTI request with course=$courseId launchId=$launchId eduTaskId=$eduTaskId: $lmsDescription")

    return super.execute(urlDecoder, request, context)
  }

  override fun getServiceName(): String = "edu/lti"

  companion object {
    private const val LAUNCH_ID = "launch_id"
    private const val LMS_DESCRIPTION = "lms_description"
  }
}