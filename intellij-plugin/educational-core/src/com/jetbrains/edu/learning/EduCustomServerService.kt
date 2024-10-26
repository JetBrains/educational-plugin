package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.authUtils.OAuthRestService
import com.jetbrains.edu.learning.builtInServer.createServerBootstrap
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.EDU_PREFIX
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.HttpRequestHandler
import org.jetbrains.io.ChannelRegistrar
import org.jetbrains.io.DelegatingHttpRequestHandlerBase
import org.jetbrains.io.NettyUtil
import java.net.InetAddress

@Service
class EduCustomServerService : Disposable {

  private var isCustomServerStarted: Boolean = false

  fun startCustomServer() {
    if (isCustomServerStarted) return
    synchronized(this) {
      if (isCustomServerStarted) return
      ApplicationManager.getApplication().executeOnPooledThread {
        startCustomServerInner()
      }
      isCustomServerStarted = true
    }
  }

  private fun startCustomServerInner() {
    val builtInServerManager = BuiltInServerManager.getInstance()
    builtInServerManager.waitForStart()
    val bootstrap = createServerBootstrap()
    bootstrap.childHandler(object : ChannelInitializer<Channel>() {
      override fun initChannel(channel: Channel) {
        val pipeline = channel.pipeline()
        NettyUtil.addHttpServerCodec(pipeline)
        pipeline.addLast(object : DelegatingHttpRequestHandlerBase() {
          override fun process(context: ChannelHandlerContext,
                               request: FullHttpRequest,
                               urlDecoder: QueryStringDecoder): Boolean {
            val path = urlDecoder.path()
            if (!path.contains(EDU_PREFIX)) return false

            val restService = HttpRequestHandler.EP_NAME.findFirstSafe {
              it is OAuthRestService && it.isSupported(request)
            } as? OAuthRestService ?: error("No handler found for request $path")

            try {
              restService.execute(urlDecoder, request, context)
            }
            catch (e: Throwable) {
              LOG.error(e)
            }
            return true
          }
        })
      }
    })
    val port = CustomAuthorizationServer.availablePort ?: error("No available port for rest server in Android Studio")
    val serverChannel = bootstrap.bind(InetAddress.getLoopbackAddress(), port).syncUninterruptibly().channel()

    val channelRegistrar = ChannelRegistrar()
    channelRegistrar.setServerChannel(serverChannel, false)

    Disposer.register(this) { channelRegistrar.close() }
  }

  override fun dispose() {}

  companion object {
    private val LOG: Logger = Logger.getInstance(EduCustomServerService::class.java)

    fun getInstance(): EduCustomServerService = service()
  }
}
