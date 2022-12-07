package com.jetbrains.edu.learning.marketplace

import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest

class MarketplaceRestServiceImpl : MarketplaceRestService() {

  override fun processCodeFlowOAuth(code: String, context: ChannelHandlerContext, request: FullHttpRequest): String? {
    error("code flow oauth not supported")
  }
}