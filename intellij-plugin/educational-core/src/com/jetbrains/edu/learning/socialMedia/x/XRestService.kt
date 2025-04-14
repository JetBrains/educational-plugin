package com.jetbrains.edu.learning.socialMedia.x

import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.origin
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import java.io.IOException
import java.lang.reflect.InvocationTargetException

class XRestService : OAuthRestService(XUtils.PLATFORM_NAME) {

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
    return if (request.method() === HttpMethod.GET &&
               // If isOriginAllowed is `false` check if it is a valid oAuth request with empty origin
               ((isOriginAllowed(request) == OriginCheckResult.ALLOW || XConnector.getInstance().isValidOAuthRequest(request, urlDecoder)))
    ) {
      true
    }
    else {
      super.isHostTrusted(request, urlDecoder)
    }
  }

  @Throws(IOException::class)
  override fun execute(
    urlDecoder: QueryStringDecoder,
    request: FullHttpRequest,
    context: ChannelHandlerContext
  ): String? {
    val result = UriMatcher(XConnector.getInstance().oAuthServicePath).match(urlDecoder)
    return when (result) {
      is ErrorUriMatchResult -> sendErrorResponse(request, context, "Failed to log in")
      is SuccessUriMatchResult -> {
        val success = XConnector.getInstance().login(result.code)
        if (success) {
          LOG.info("$platformName: OAuth code is handled")
          return sendOkResponse(request, context)
        }
        return sendErrorResponse(request, context, "Failed to log in using provided code")
      }
      null -> {
        sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
        "Unknown command: ${urlDecoder.uri()}"
      }
    }
  }

  override fun getServiceName(): String = XConnector.getInstance().serviceName

  override fun isOriginAllowed(request: HttpRequest): OriginCheckResult {
    val originAllowed = super.isOriginAllowed(request)
    if (originAllowed == OriginCheckResult.FORBID) {
      val origin = request.origin ?: return OriginCheckResult.FORBID
      if (origin == XConnector.AUTH_BASE_URL) {
        return OriginCheckResult.ALLOW
      }
    }
    return originAllowed
  }

  override fun isAccessible(request: HttpRequest): Boolean {
    return super.isAccessible(request) && Registry.`is`("edu.socialMedia.x.oauth2")
  }
}

// TODO: refactor `EduLoginConnector` and the corresponding `OAuthRestService`s to use this instead of regex
private sealed interface UriMatchResult

private data class SuccessUriMatchResult(val code: String, val state: String) : UriMatchResult
private data class ErrorUriMatchResult(val error: String) : UriMatchResult

private class UriMatcher(private val path: String) {

  fun match(urlDecoder: QueryStringDecoder): UriMatchResult? {
    if (urlDecoder.path() != path) return null
    val params = urlDecoder.parameters()
    return when (params.size) {
      1 -> {
        val error = params["error"]?.singleOrNull() ?: return null
        ErrorUriMatchResult(error)
      }
      2 -> {
        val code = params["code"]?.singleOrNull() ?: return null
        val state = params["state"]?.singleOrNull() ?: return null
        SuccessUriMatchResult(code, state)
      }
      else -> null
    }
  }
}
