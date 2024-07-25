package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder

class MarketplaceRestService : BaseMarketplaceRestService(MARKETPLACE) {

  override fun preProcessRequest(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext) {}

  override fun getServiceName(): String = MarketplaceConnector.getInstance().serviceName

}