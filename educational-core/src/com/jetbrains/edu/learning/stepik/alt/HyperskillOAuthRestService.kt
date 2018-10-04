package com.jetbrains.edu.learning.stepik.alt

import com.jetbrains.edu.learning.authUtils.OAuthRestService
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.RestService
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.util.regex.Pattern

class HyperskillOAuthRestService : OAuthRestService("Hyperskill") {

  override fun getServiceName(): String = EDU_HYPERSKILL_SERVICE_NAME

  override fun isMethodSupported(method: HttpMethod): Boolean =
    method === HttpMethod.GET

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(request: FullHttpRequest): Boolean {
    val uri = request.uri()
    val codeMatcher = OAUTH_CODE_PATTERN.matcher(uri)
    return if (request.method() === HttpMethod.GET && codeMatcher.matches()) {
      true
    }
    else super.isHostTrusted(request)
  }

  @Throws(IOException::class)
  override fun execute(decoder: QueryStringDecoder,
                       request: FullHttpRequest,
                       context: ChannelHandlerContext): String? {
    val uri = decoder.uri()

    if (OAUTH_CODE_PATTERN.matcher(uri).matches()) {
      val code = RestService.getStringParameter("code", decoder)!! // cannot be null because of pattern

      val success = HyperskillConnector.login(code)
      if (success) {
        RestService.LOG.info("$myPlatformName: OAuth code is handled")
        return sendOkResponse(request, context)
      }
      return sendErrorResponse(request, context, "Failed to login using provided code")
    }

    RestService.sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    return "Unknown command: $uri"
  }

  companion object {
    const val EDU_HYPERSKILL_SERVICE_NAME = "edu/hyperskill/oauth"
    private val OAUTH_CODE_PATTERN = Pattern.compile("/api/$EDU_HYPERSKILL_SERVICE_NAME\\?code=(\\w+)")
  }
}
