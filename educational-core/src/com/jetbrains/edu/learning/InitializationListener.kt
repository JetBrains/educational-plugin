package com.jetbrains.edu.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.PathUtil
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.learning.authUtils.OAuthUtils.isBuiltinPortValid
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.ui.SelectRolePanel
import com.jetbrains.edu.learning.yaml.YamlDeserializerFactory
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import kotlinx.coroutines.runBlocking
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
    if (EduUtils.isAndroidStudio()) {
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

    if (!CCPluginToggleAction.isCourseCreatorFeaturesPropertySet) {
      @Suppress("UnstableApiUsage", "DEPRECATION")
      if (!PlatformUtils.isPyCharmEducational() && !PlatformUtils.isIdeaEducational()) {
        CCPluginToggleAction.isCourseCreatorFeaturesEnabled = true
      }
      else {
        // HACK: ActionManager is instantiated here
        // otherwise it is instantiated during dialog showing (to render buttons on Mac OS magic touch bar)
        // which causes assert because one shouldn't instantiate ActionManager in EDT
        ActionManager.getInstance()

        runBlocking(AppUIExecutor.onUiThread().coroutineDispatchingContext()) {
          showInitialConfigurationDialog()
        }
      }
    }
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
      YamlDeserializerFactory.getDefaultDeserializer().deserializeItem(courseConfig, null) as? Course
    }
  }

  private fun showInitialConfigurationDialog() {
    val dialog = DialogBuilder()
    val panel = SelectRolePanel()
    dialog.setPreferredFocusComponent(panel.getStudentButton())
    dialog.title(EduCoreBundle.message("select.role.dialog.title")).centerPanel(panel)
    dialog.addOkAction().setText(EduCoreBundle.message("select.role.dialog.ok.action", StepikNames.PLUGIN_NAME))
    dialog.show()
  }

  private fun notifyUnsupportedPort(port: Int) {
    Notification(
      "EduTools",
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
  }
}
