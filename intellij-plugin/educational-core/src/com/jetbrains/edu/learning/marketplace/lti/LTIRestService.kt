package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.authUtils.hasOpenDialogs
import com.jetbrains.edu.learning.marketplace.BaseMarketplaceRestService
import com.jetbrains.edu.learning.marketplace.LTI
import com.jetbrains.edu.learning.marketplace.MARKETPLACE
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder

class LTIRestService : BaseMarketplaceRestService(LTI) {

  override fun preProcessRequest(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext) {
    if (hasOpenDialogs(MARKETPLACE)) {
      sendOk(request, context)
      return
    }

    val courseId = getIntParameter(MARKETPLACE_COURSE_ID, urlDecoder)
    val launchId = getStringParameter(LAUNCH_ID, urlDecoder)
    val studyItemId = getStringParameter(STUDY_ITEM_ID, urlDecoder)
    val lmsDescription = getStringParameter(LMS_DESCRIPTION, urlDecoder)
    logger<LTIRestService>().info("LTI request with course=$courseId launchId=$launchId studyItemId=$studyItemId: $lmsDescription")
  }

  override fun getServiceName(): String = "edu/lti"

  companion object {
    private const val MARKETPLACE_COURSE_ID = "marketplace_course_id"
    private const val LAUNCH_ID = "launch_id"
    private const val STUDY_ITEM_ID = "study_item_id"
    private const val LMS_DESCRIPTION = "lms_description"
  }
}