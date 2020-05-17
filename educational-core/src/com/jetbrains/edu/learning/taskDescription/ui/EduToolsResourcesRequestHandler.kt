package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.getResource
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.*
import io.netty.handler.stream.ChunkedStream
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.HttpRequestHandler
import org.jetbrains.io.FileResponses
import org.jetbrains.io.addCommonHeaders
import org.jetbrains.io.addKeepAliveIfNeeded
import java.io.ByteArrayInputStream
import java.util.*

/**
 * Used for resolving local resources as remote ones in JCEF
 */
class EduToolsResourcesRequestHandler : HttpRequestHandler() {
  override fun process(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): Boolean {
    val uri = request.uri()
    if (!uri.contains(EDU_RESOURCES)) {
      return false
    }

    val resourceRelativePath = uri.split(EDU_RESOURCES)[1]
    if (resourceRelativePath !in StyleResourcesManager.resourcesList) return false
    val url = getResource(resourceRelativePath) ?: return false
    val bytes = url.readBytes()
    return sendData(bytes, url.file, request, context.channel())
  }

  private fun sendData(content: ByteArray, name: String, request: FullHttpRequest, channel: Channel): Boolean {
    val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
    response.addCommonHeaders()
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, FileResponses.getContentType(name))
    response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, must-revalidate")
    response.headers().set(HttpHeaderNames.LAST_MODIFIED, Date(Calendar.getInstance().timeInMillis))

    val keepAlive = response.addKeepAliveIfNeeded(request)
    if (request.method() != HttpMethod.HEAD) {
      HttpUtil.setContentLength(response, content.size.toLong())
    }

    channel.write(response)

    if (request.method() != HttpMethod.HEAD) {
      val stream = ByteArrayInputStream(content)
      channel.write(ChunkedStream(stream))
      stream.close()
    }

    val future = channel.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT)
    if (!keepAlive) {
      future.addListener(ChannelFutureListener.CLOSE)
    }
    return true
  }

  companion object {
    val LOG: Logger = Logger.getInstance(EduToolsResourcesRequestHandler::class.java)

    const val EDU_RESOURCES: String = "eduResources"

    fun resourceWebUrl(name: String): String {
      val resource = getResource(name)
      if (resource == null) {
        LOG.warn("Cannot find resource: $name")
        return ""
      }

      val port = BuiltInServerManager.getInstance().port
      return "http://localhost:$port/$EDU_RESOURCES/${name.trimStart('/')}"
    }

  }
}