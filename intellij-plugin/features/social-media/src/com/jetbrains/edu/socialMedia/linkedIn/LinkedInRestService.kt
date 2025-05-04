package com.jetbrains.edu.socialMedia.linkedIn

import com.intellij.util.io.origin
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.authUtils.createResponse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODE_ARGUMENT
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import org.jetbrains.io.send
import java.io.IOException
import java.lang.reflect.InvocationTargetException

class LinkedInRestService : OAuthRestService("LinkedIn") {

  @Throws(InterruptedException::class, InvocationTargetException::class)
  override fun isHostTrusted(request: FullHttpRequest, urlDecoder: QueryStringDecoder): Boolean {
    return if (request.method() === HttpMethod.GET
               // If isOriginAllowed is `false` check if it is a valid oAuth request with empty origin
               && ((isOriginAllowed(request) === OriginCheckResult.ALLOW || LinkedInConnector.getInstance()
        .isValidOAuthRequest(request, urlDecoder)))
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
    val uri = urlDecoder.uri()

    if (LinkedInConnector.getInstance().getOAuthPattern("\\?error=(\\w+)").matcher(uri).matches()) {
      return sendErrorResponse(request, context, "Failed to log in")
    }

    if (LinkedInConnector.getInstance().getOAuthPattern().matcher(uri).matches()) {
      val code = getStringParameter(CODE_ARGUMENT, urlDecoder)!! // cannot be null because of pattern
      val success = LinkedInConnector.getInstance().login(code)
      if (success) {
        LOG.info("$platformName: OAuth code is handled")
        val pageContent = getInternalTemplateText("linkedin.redirectPage.html")
        createResponse(pageContent).send(context.channel(), request)
        return null
      }
      return sendErrorResponse(request, context, "Failed to log in using provided code")
    }

    sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    return "Unknown command: $uri"
  }

  override fun getServiceName(): String = LinkedInConnector.getInstance().serviceName

  override fun isOriginAllowed(request: HttpRequest): OriginCheckResult {
    val originAllowed = super.isOriginAllowed(request)
    if (originAllowed == OriginCheckResult.FORBID) {
      val origin = request.origin ?: return OriginCheckResult.FORBID
      if (origin == LinkedInConnector.LINKEDIN_BASE_WWW_URL) {
        return OriginCheckResult.ALLOW
      }
    }
    return originAllowed
  }
}
