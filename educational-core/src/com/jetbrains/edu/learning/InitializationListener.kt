package com.jetbrains.edu.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.BrowserUtil
import com.intellij.ide.IdeBundle
import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.PathUtil
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.authUtils.OAuthUtils.isBuiltinPortValid
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.JBCoursesStorage
import com.jetbrains.edu.learning.yaml.YamlDeserializer
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import org.jetbrains.ide.BuiltInServerManager
import java.io.File

class InitializationListener : AppLifecycleListener, DynamicPluginListener {

  override fun appFrameCreated(commandLineArgs: List<String>) {
    init()
  }

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    if (pluginDescriptor.pluginId.idString == EduNames.PLUGIN_ID) {
      init()
    }
  }

  private fun init() {
    if (EduUtilsKt.isAndroidStudio()) {
      // Android Studio requires additional credentials to use builtin server:
      // login/password form is opened in browser when a query to builtin server is made,
      // one should use _token_ as login and token from <config>/user.token file as password
      // (see DelegatingHttpRequestHandler file in Android Studio sources for more details).
      // This is unacceptable in terms of UX for our Hyperskill integration.
      // That's why we start custom server on another port to handle Hyperskill related queries.
      EduCustomServerService.getInstance().startCustomServer()
    }
    if (isUnitTestMode) return

    val port = BuiltInServerManager.getInstance().port
    if (!isBuiltinPortValid(port)) {
      notifyUnsupportedPort(port)
    }

    val propertiesComponent = PropertiesComponent.getInstance()
    if (!propertiesComponent.isValueSet(RECENT_COURSES_FILLED)) {
      fillRecentCourses()
      propertiesComponent.setValue(RECENT_COURSES_FILLED, true)
    }

    if (!propertiesComponent.isValueSet(STEPIK_AUTH_RESET)) {
      EduSettings.getInstance().user = null
      propertiesComponent.setValue(STEPIK_AUTH_RESET, true)
    }

    @Suppress("UnstableApiUsage", "DEPRECATION")
    if (PlatformUtils.isPyCharmEducational() || PlatformUtils.isIdeaEducational()) {
      showSwitchFromEduNotification()
    }
  }

  private fun showSwitchFromEduNotification() {
    val notification = Notification(
      "JetBrains Academy",
      EduCoreBundle.message("notification.ide.switch.from.edu.ide.title", ApplicationNamesInfo.getInstance().fullProductNameWithEdition),
      EduCoreBundle.message("notification.ide.switch.from.edu.ide.description",
                            "${ApplicationNamesInfo.getInstance().fullProductName} Community"),
      NotificationType.ERROR,
    ).apply {
      isSuggestionType = true
      configureDoNotAskOption(SWITCH_TO_COMMUNITY_DO_NOT_ASK_OPTION_ID,
                              EduCoreBundle.message("notification.ide.switch.from.edu.ide.do.not.ask"))
    }

    notification
      .addAction(
        NotificationAction.createSimple(EduCoreBundle.message("notification.ide.switch.from.edu.ide.acton.title")) {
          @Suppress("UnstableApiUsage")
          val link = if (PlatformUtils.isPyCharmEducational()) {
            "https://www.jetbrains.com/pycharm/download/"
          }
          else {
            "https://www.jetbrains.com/idea/download/"
          }
          BrowserUtil.browse(link)
          notification.expire()
        })
      .addAction(NotificationAction.createSimple((IdeBundle.message("notifications.toolwindow.dont.show.again"))) {
        @Suppress("UnstableApiUsage")
        notification.setDoNotAskFor(null)
        notification.expire()
      })
    notification.notify(null)
  }

  private fun fillRecentCourses() {
    val state = RecentProjectsManagerBase.getInstanceEx().state
    val recentPathsInfo = state.additionalInfo
    recentPathsInfo.forEach {
      val projectPath = it.key
      val course = deserializeCourse(projectPath)
      if (course != null) {
        // Note: we don't set course progress here, because we didn't load course items here
        JBCoursesStorage.getInstance().addCourse(course, projectPath)
      }
    }
  }

  private fun deserializeCourse(projectPath: String): Course? {
    val projectFile = File(PathUtil.toSystemDependentName(projectPath))
    val projectDir = VfsUtil.findFile(projectFile.toPath(), true) ?: return null
    val courseConfig = projectDir.findChild(YamlFormatSettings.COURSE_CONFIG) ?: return null
    return runReadAction {
      YamlDeserializer.deserializeItem(courseConfig, null) as? Course
    }
  }

  private fun notifyUnsupportedPort(port: Int) {
    Notification(
      "JetBrains Academy",
      EduNames.JBA,
      EduCoreBundle.message("hyperskill.unsupported.port.extended.message", port.toString(), EduNames.OUTSIDE_OF_KNOWN_PORT_RANGE_URL),
      NotificationType.ERROR,
    )
      .setListener(NotificationListener.URL_OPENING_LISTENER)
      .notify(null)
  }

  companion object {
    const val RECENT_COURSES_FILLED = "Educational.recentCoursesFilled"
    const val STEPIK_AUTH_RESET = "Educational.stepikOAuthReset"
    private const val SWITCH_TO_COMMUNITY_DO_NOT_ASK_OPTION_ID = "Edu IDEs aren't supported"
  }
}