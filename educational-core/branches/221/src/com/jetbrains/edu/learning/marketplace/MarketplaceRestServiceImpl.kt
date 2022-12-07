package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.authUtils.createResponse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import org.jetbrains.io.send

class MarketplaceRestServiceImpl : MarketplaceRestService() {

  override fun processCodeFlowOAuth(code: String, context: ChannelHandlerContext, request: FullHttpRequest): String? {
    val success = MarketplaceConnector.getInstance().login(code)
    if (success) {
      LOG.info("$myPlatformName: OAuth code is handled")
      val pageContent = GeneratorUtils.getInternalTemplateText("marketplace.redirectPage.html")
      createResponse(pageContent).send(context.channel(), request)
      return null
    }
    return sendErrorResponse(request, context, "Failed to log in using provided code")
  }
}