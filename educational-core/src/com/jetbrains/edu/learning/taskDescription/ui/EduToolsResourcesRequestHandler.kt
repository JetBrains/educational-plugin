package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.JavaUILibrary.Companion.isJCEF
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
import java.net.URL
import java.util.*

class EduToolsResourcesRequestHandler : HttpRequestHandler() {
  override fun process(urlDecoder: QueryStringDecoder, request: FullHttpRequest, context: ChannelHandlerContext): Boolean {
    val uri = request.uri()
    if (!uri.contains(EDU_RESOURCES)) {
      return false
    }

    val url = getResource(uri.split(EDU_RESOURCES)[1]) ?: return false
    val bytes = url.readBytes()
    return sendData(bytes, url.file, request, context.channel())
  }

  private fun readResourceFile(file: String): ByteArray? {
    val resource = getResource(file) ?: return null
    return resource.readBytes()
  }

  private fun sendData(content: ByteArray, name: String, request: FullHttpRequest, channel: Channel): Boolean {
    val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK)
    response.headers().set(HttpHeaderNames.CONTENT_TYPE, FileResponses.getContentType(name))
    response.addCommonHeaders()
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

    /**
     * JCEF doesn't load local resources
     * Otherwise JavaFX does load only local resources, see [javafx.scene.web.WebEngine.userStyleSheetLocation]
     */
    fun eduResourceUrl(name: String): String = when {
      isJCEF() -> eduResourceWebUrl(name)
      else -> eduResourceFileUrl(name)
    }

    private fun getResource(name: String): URL? = object {}.javaClass.getResource(name)

    private fun eduResourceWebUrl(name: String): String {
      val resource = getResource(name)
      if (resource == null) {
        LOG.warn("Cannot find resource: $name")
        return ""
      }

      val port = BuiltInServerManager.getInstance().port
      return "http://localhost:$port/$EDU_RESOURCES/${name.trimStart('/')}"
    }

    private fun eduResourceFileUrl(name: String): String {
      val resource = getResource(name)?.toExternalForm()
      return if (resource != null) {
        resource
      }
      else {
        LOG.warn("Cannot find resource: $name")
        ""
      }
    }
  }
}