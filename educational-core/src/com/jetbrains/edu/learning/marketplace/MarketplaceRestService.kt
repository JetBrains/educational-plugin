package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.io.send
import java.lang.reflect.InvocationTargetException
import java.util.regex.Pattern


class MarketplaceRestService : OAuthRestService(MARKETPLACE) {
  override fun getServiceName(): String = EDU_MARKETPLACE_SERVICE_NAME

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
    val uri = request.uri()
    val codeMatcher = OAUTH_CODE_PATTERN.matcher(uri)
    val errorMatcher = OAUTH_ERROR_CODE_PATTERN.matcher(uri)
    return if (request.method() === HttpMethod.GET && (codeMatcher.matches() || errorMatcher.matches())) {
      true
    }
    else super.isHostTrusted(request, urlDecoder)
  }

  override fun execute(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    val uri = urlDecoder.uri()
    if (OAUTH_CODE_PATTERN.matcher(uri).matches()) {
      val code = getStringParameter("code", urlDecoder)!! // cannot be null because of pattern

      val success = MarketplaceConnector.getInstance().login(code)
      if (success) {
        LOG.info("$myPlatformName: OAuth code is handled")
        val pageContent = GeneratorUtils.getInternalTemplateText("marketplace.redirectPage.html")
        createResponse(pageContent).send(context.channel(), request)
        return null
      }
      return sendErrorResponse(request, context, "Failed to login using provided code")
    }

    sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    return "Unknown command: $uri"
  }

  companion object {
    const val EDU_MARKETPLACE_SERVICE_NAME = "edu/marketplace"
    private val OAUTH_CODE_PATTERN = Pattern.compile("/$PREFIX/$EDU_MARKETPLACE_SERVICE_NAME/oauth\\?code=(\\w+)")
    private val OAUTH_ERROR_CODE_PATTERN = Pattern.compile("/$PREFIX/$EDU_MARKETPLACE_SERVICE_NAME/oauth\\?error=(\\w+)")
  }
}