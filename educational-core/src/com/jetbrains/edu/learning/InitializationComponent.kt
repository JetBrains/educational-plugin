package com.jetbrains.edu.learning

import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.BaseComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.PathUtil
import com.jetbrains.edu.learning.authUtils.CustomAuthorizationServer
import com.jetbrains.edu.learning.builtInServer.createServerBootstrap
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.editor.EduEditorFactoryListener
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillRestService
import com.jetbrains.edu.learning.update.NewCoursesNotifier
import com.jetbrains.edu.learning.yaml.YamlDeserializer
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
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
import java.io.File
import java.net.InetAddress

@Suppress("ComponentNotRegistered") // registered in educational-core.xml
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

    if (!PropertiesComponent.getInstance().isValueSet(RECENT_COURSES_FILLED)) {
      fillRecentCourses()
      PropertiesComponent.getInstance().setValue(RECENT_COURSES_FILLED, true)
    }

    newCoursesNotifier.scheduleNotification()
  }

  private fun fillRecentCourses() {
    val state = RecentProjectsManagerBase.instanceEx.state
    val recentPathsInfo = state.additionalInfo
    recentPathsInfo.forEach {
      val projectPath = it.key
      val course = deserializeCourse(projectPath)
      if (course != null) {
        // Note: we don't set course progress here, because we didn't load course items here
        CoursesStorage.getInstance().addCourse(course, projectPath)
      }
    }
  }

  private fun deserializeCourse(projectPath: String) : Course? {
    val projectFile = File(PathUtil.toSystemDependentName(projectPath))
    val projectDir = VfsUtil.findFile(projectFile.toPath(), true) ?: return null
    val courseConfig = projectDir.findChild(YamlFormatSettings.COURSE_CONFIG) ?: return null
    return runReadAction {
       YamlDeserializer.deserializeItem(courseConfig, null) as? Course
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

  companion object {
    const val RECENT_COURSES_FILLED = "Educational.recentCoursesFilled"
  }
}
