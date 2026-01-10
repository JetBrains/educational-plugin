package com.jetbrains.edu.learning.marketplace

import com.intellij.notification.NotificationType
import com.intellij.util.io.origin
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.authUtils.hasOpenDialogs
import com.jetbrains.edu.learning.authUtils.sendPluginInfoResponse
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenCourseRequestBase
import com.jetbrains.edu.learning.marketplace.courseGeneration.MarketplaceOpenInIdeRequestHandler
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.showNotificationFromCourseValidation
import com.jetbrains.edu.learning.notification.EduNotificationManager
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.QueryStringDecoder
import java.util.regex.Pattern

abstract class MarketplaceRestServiceBase<Request : MarketplaceOpenCourseRequestBase>(platformName: String) : OAuthRestService(platformName) {

  override fun execute(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): String? {
    if (urlDecoder.path().contains(INFO)) {
      sendPluginInfoResponse(request, context)
      return null
    }

    if (hasOpenDialogs(platformName)) {
      sendOk(request, context)
      return null
    }

    createMarketplaceOpenCourseRequest(urlDecoder).map { marketplaceRequest ->
      openInIDE(marketplaceRequest, request, context)
      LOG.info("Received marketplace request: $marketplaceRequest")
    }.onError { error ->
      LOG.warn("Error while processing marketplace request: $error")
      EduNotificationManager.create(
        NotificationType.ERROR,
        EduCoreBundle.message("rest.service.marketplace.request.error"),
        error
      ).notify(null)
    }

    sendStatus(HttpResponseStatus.BAD_REQUEST, false, context.channel())
    return "Unknown command: ${urlDecoder.uri()}"
  }

  protected abstract fun createMarketplaceOpenCourseRequest(urlDecoder: QueryStringDecoder): Result<Request, String>

  private fun openInIDE(
    openCourseRequest: Request,
    request: FullHttpRequest,
    context: ChannelHandlerContext
  ): String? {
    LOG.info("Opening $platformName course: $openCourseRequest")
    return when (val result = ProjectOpener.getInstance().open(MarketplaceOpenInIdeRequestHandler, openCourseRequest)) {
      is Ok -> {
        sendOk(request, context)
        LOG.info("$platformName course opened: $openCourseRequest")
        null
      }

      is Err -> {
        val validationResult = result.error
        val message = validationResult.message
        LOG.warn(message)
        showNotificationFromCourseValidation(
          validationResult,
          EduCoreBundle.message("notification.title.failed.to.open.in.ide", openCourseRequest)
        )
        sendStatus(HttpResponseStatus.NOT_FOUND, false, context.channel())
        message
      }
    }
  }

  override fun isOriginAllowed(request: HttpRequest): OriginCheckResult {
    val originAllowed = super.isOriginAllowed(request)
    if (originAllowed == OriginCheckResult.FORBID) {
      val origin = request.origin ?: return OriginCheckResult.FORBID
      return if (TRUSTED_ORIGINS.contains(origin) || JETBRAINS_ORIGIN_PATTERN.matcher(origin).matches()) OriginCheckResult.ALLOW
      else OriginCheckResult.ASK_CONFIRMATION
    }
    return originAllowed
  }

  companion object {
    internal const val INFO = "info"
    const val EDU_TASK_ID = "edu_task_id"
    const val STUDY_ITEM_ID = "edu_study_item_id"
    private val JETBRAINS_ORIGIN_PATTERN = Pattern.compile("https://([a-z0-9-]+\\.)*jetbrains.com$")
    private val TRUSTED_ORIGINS = setOf(PLUGINS_REPOSITORY_URL, PLUGINS_EDU_DEMO, PLUGINS_MASTER_DEMO)
  }
}