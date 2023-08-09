package com.jetbrains.edu.learning.checkio.connectors

import com.intellij.util.io.origin
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.checkio.utils.CheckiONames.CHECKIO_URL
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CHECKIO
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.regex.Pattern

abstract class CheckiOOAuthRestService(platformName: String, private val oAuthConnector: CheckiOOAuthConnector) :
  OAuthRestService(platformName) {
  private val oAuthCodePattern: Pattern = oAuthConnector.getOAuthPattern()

  override fun getServiceName(): String = oAuthConnector.serviceName

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
    return if (request.method() === HttpMethod.GET
               // If isOriginAllowed is `false` check if it is a valid oAuth request with empty origin
               && (isOriginAllowed(request) === OriginCheckResult.ALLOW || oAuthConnector.isValidOAuthRequest(request, urlDecoder))) {
      true
    }
    else super.isHostTrusted(request, urlDecoder)
  }

  @Throws(IOException::class)
  override fun execute(
    urlDecoder: QueryStringDecoder,
    request: FullHttpRequest,
    context: ChannelHandlerContext
  ): String? {
    val uri = urlDecoder.uri()
    LOG.info("Request: $uri")
    if (oAuthCodePattern.matcher(uri).matches()) {
      // cannot be null because of pattern
      val code = getStringParameter(CODE_ARGUMENT, urlDecoder)!!
      LOG.info("$platformName: OAuth code is handled")
      val success = oAuthConnector.login(code)
      return if (success) {
        sendOkResponse(request, context)
      }
      else {
        val errorMessage = "Failed to login to $CHECKIO"
        sendErrorResponse(request, context, errorMessage)
      }
    }
    sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    return "Unknown command: $uri"
  }

  override fun isOriginAllowed(request: HttpRequest): OriginCheckResult {
    val originAllowed = super.isOriginAllowed(request)
    if (originAllowed == OriginCheckResult.FORBID) {
      val origin = request.origin ?: return OriginCheckResult.FORBID
      return if (origin == CHECKIO_URL) OriginCheckResult.ALLOW
      else OriginCheckResult.ASK_CONFIRMATION
    }
    return originAllowed
  }
}
