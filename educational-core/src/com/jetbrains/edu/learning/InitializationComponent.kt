package com.jetbrains.edu.learning

import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.builtInServer.createServerBootstrap
import com.jetbrains.edu.learning.editor.EduEditorFactoryListener
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillRestService
import com.jetbrains.edu.learning.update.NewCoursesNotifier
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.QueryStringDecoder
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.BuiltInServerManagerImpl
import org.jetbrains.ide.HttpRequestHandler
import org.jetbrains.io.ChannelRegistrar
import org.jetbrains.io.DelegatingHttpRequestHandlerBase
import org.jetbrains.io.NettyUtil
import org.jetbrains.io.send
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.*

class InitializationComponent : BaseComponent {

    private val newCoursesNotifier = NewCoursesNotifier(ApplicationManager.getApplication())

    override fun initComponent() {
        if (EduUtils.isAndroidStudio()) {
            // Android Studio requires additional credentials to use builtin server:
            // login/password form is opened in browser when a query to builtin server is made,
            // one should use _token_ as login and token from <config>/user.token file as password
            // (see DelegatingHttpRequestHandler file in Android Studio sources for more details).
            // This is unacceptable in terms of UX for our Hyperskill integration.
            // That's why we start custom server on another port to handle Hyperskill related queries.
            ApplicationManager.getApplication().executeOnPooledThread {
                startCustomServer()
            }
        }

        //Register placeholder size listener
        EditorFactory.getInstance().addEditorFactoryListener(EduEditorFactoryListener(), ApplicationManager.getApplication())

        if (isUnitTestMode) return

        if (PropertiesComponent.getInstance().isValueSet(CONFLICTING_PLUGINS_DISABLED)) {
            newCoursesNotifier.scheduleNotification()
            return
        }

        // Remove conflicting plugins
        var disabledPlugins = disablePlugins()
        if (disabledPlugins.isNotEmpty()) {
            disabledPlugins = disabledPlugins.map { name -> "'$name'" }
            val multiplePluginsDisabled = disabledPlugins.size != 1

            val names = if (multiplePluginsDisabled) StringUtil.join(disabledPlugins, ", ") else disabledPlugins[0]
            val ending = if (multiplePluginsDisabled) "s" else ""
            val verb = if (multiplePluginsDisabled) "were" else "was"

            restartIDE("Conflicting plugin$ending $names $verb disabled")
        } else {
            PropertiesComponent.getInstance().setValue(CONFLICTING_PLUGINS_DISABLED, "true")
            newCoursesNotifier.scheduleNotification()
        }
    }

    private fun startCustomServer() {
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
                        if (!urlDecoder.path().contains(HyperskillRestService.EDU_HYPERSKILL_SERVICE_NAME)) {
                            return false
                        }
                        val hyperskillRestService = HttpRequestHandler.EP_NAME.findExtension(HyperskillRestService::class.java) ?: error(
                          "No handler for Hyperskill")
                        try {
                            hyperskillRestService.execute(urlDecoder, request, context)
                        }
                        catch (e: Throwable) {
                            Logger.getInstance(InitializationComponent::class.java).error(e)
                        }
                        return true
                    }
                })
            }
        })
        val port = CustomAuthorizationServer.getAvailablePort()
        if (port == -1) {
            error("No available port for Hyperskill server in Android Studio")
        }
        val serverChannel = bootstrap.bind(InetAddress.getLoopbackAddress(), port).syncUninterruptibly().channel()

        val channelRegistrar = ChannelRegistrar()
        channelRegistrar.setServerChannel(serverChannel, false)

        Disposer.register(ApplicationManager.getApplication(), Disposable { channelRegistrar.close() })
    }

    private fun disablePlugins(): List<String> {
        val disabledPlugins = ArrayList<String>()
        for (id in IDS) {
            val plugin = PluginManager.getPlugin(PluginId.getId(id)) ?: continue
            if (plugin.isEnabled) {
                disabledPlugins.add(plugin.name)
                PluginManagerCore.disablePlugin(id)
            }
        }
        return disabledPlugins
    }

    companion object {
        @JvmField
        val IDS = arrayOf(
          "com.jetbrains.edu.intellij",
          "com.jetbrains.edu.interactivelearning",
          "com.jetbrains.python.edu.interactivelearning.python",
          "com.jetbrains.edu.coursecreator",
          "com.jetbrains.edu.coursecreator.python",
          "com.jetbrains.edu.kotlin",
          "com.jetbrains.edu.coursecreator.intellij",
          "com.jetbrains.edu.java",
          "com.jetbrains.python.edu.core",
          "com.jetbrains.edu.core"
        )
        const val CONFLICTING_PLUGINS_DISABLED = "Educational.conflictingPluginsDisabled"
    }
}
