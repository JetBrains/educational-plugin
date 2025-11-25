package com.jetbrains.edu.learning

import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.BrowserUtil
import com.intellij.ide.IdeBundle
import com.intellij.ide.RecentProjectsManagerBase
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.Anchor
import com.intellij.openapi.actionSystem.Constraints
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.PathUtil
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.agreement.UserAgreementManager
import com.jetbrains.edu.learning.authUtils.OAuthUtils.isBuiltinPortValid
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeCourse
import com.jetbrains.edu.learning.yaml.YamlMapper
import org.intellij.lang.annotations.Language
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
    UserAgreementManager.getInstance()
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

    setupEduActionsLocation()
  }

  private fun showSwitchFromEduNotification() {
    EduNotificationManager.create(
      ERROR,
      EduCoreBundle.message("notification.ide.switch.from.edu.ide.title", ApplicationNamesInfo.getInstance().fullProductNameWithEdition),
      EduCoreBundle.message(
        "notification.ide.switch.from.edu.ide.description",
        "${ApplicationNamesInfo.getInstance().fullProductName} Community"
      ),
    ).apply {
      isSuggestionType = true
      configureDoNotAskOption(
        SWITCH_TO_COMMUNITY_DO_NOT_ASK_OPTION_ID,
        EduCoreBundle.message("notification.ide.switch.from.edu.ide.do.not.ask")
      )
      addAction(
        NotificationAction.createSimple(EduCoreBundle.message("notification.ide.switch.from.edu.ide.acton.title")) {
          @Suppress("UnstableApiUsage")
          val link = if (PlatformUtils.isPyCharmEducational()) {
            "https://www.jetbrains.com/pycharm/download/"
          }
          else {
            "https://www.jetbrains.com/idea/download/"
          }
          BrowserUtil.browse(link)
          this@apply.expire()
        })
      addAction(NotificationAction.createSimple((IdeBundle.message("notifications.toolwindow.dont.show.again"))) {
        @Suppress("UnstableApiUsage")
        this@apply.setDoNotAskFor(null)
        this@apply.expire()
      })
    }.notify(null)
  }

  private fun fillRecentCourses() {
    val state = RecentProjectsManagerBase.getInstanceEx().state
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

  private fun deserializeCourse(projectPath: String): Course? {
    val projectFile = File(PathUtil.toSystemDependentName(projectPath))
    val projectDir = VfsUtil.findFile(projectFile.toPath(), true) ?: return null
    val courseConfig = projectDir.findChild(YamlConfigSettings.COURSE_CONFIG) ?: return null
    return runReadAction {
      ProgressManager.getInstance().computeInNonCancelableSection<Course, Exception> {
        YamlMapper.basicMapper().deserializeCourse(VfsUtil.loadText(courseConfig))
      }
    }
  }

  private fun notifyUnsupportedPort(port: Int) {
    EduNotificationManager.create(
      ERROR,
      EduNames.JBA,
      EduCoreBundle.message("hyperskill.unsupported.port.extended.message", port.toString(), EduNames.OUTSIDE_OF_KNOWN_PORT_RANGE_URL)
    ).setListener(NotificationListener.URL_OPENING_LISTENER)
      .notify(null)
  }

  /**
   * Sets up location (groups) for some JetBrains plugin actions in runtime.
   * Do it in this way because the declarative way via plugin manifest doesn't work well
   * in cases of custom action groups in IDE UI or overridden existing action groups.
   */
  private fun setupEduActionsLocation() {
    // Rider customizes some of its action groups, so common locations don't work here
    if (PlatformUtils.isRider()) {
      val actionManager = ActionManagerEx.getInstanceEx()

      actionManager.addActionToGroup(
        "Educational.Educator.NewFile",
        "SolutionViewAddGroup.SolutionSection",
        Constraints.FIRST
      )

      actionManager.addActionToGroup(
        "Educational.Educator.CourseCreator.FrameworkLesson",
        "SolutionExplorerPopupMenu",
        relativeConstraints(Anchor.BEFORE, "RunContextGroupInner")
      )

      actionManager.addActionToGroup(
        "Educational.LearnAndTeachFileMenu",
        "FileMenu",
        relativeConstraints(Anchor.BEFORE, "RiderFileOpenGroup")
      )

      actionManager.addActionToGroup(
        "Educational.Educator.CourseCreator.Menu",
        "SolutionExplorerPopupMenu.Edit",
        Constraints.LAST
      )
      actionManager.addActionToGroup(
        "Educational.Educator.CourseCreator.Menu",
        "SolutionExplorerPopupMenu",
        relativeConstraints(Anchor.AFTER, "Educational.Educator.CourseCreator.FrameworkLesson")
      )
      actionManager.addActionToGroup(
        "Educational.Educator.CourseCreator.Menu",
        "FileMenu",
        relativeConstraints(Anchor.AFTER, "Educational.LearnAndTeachFileMenu")
      )
    }
  }

  private fun ActionManagerEx.addActionToGroup(
    @Language("devkit-action-id") actionId: String,
    @Language("devkit-action-id") groupId: String,
    constraints: Constraints
  ) {
    val action = getAction(actionId)
    if (action == null) {
      LOG.warn("Action `$actionId` not found")
      return
    }
    val group = getAction(groupId) as? ActionGroup
    if (group == null) {
      LOG.warn("Action group `$groupId` not found")
      return
    }
    asActionRuntimeRegistrar().addToGroup(group, action, constraints)
  }

  private fun relativeConstraints(anchor: Anchor, @Language("devkit-action-id") actionId: String): Constraints {
    require(anchor == Anchor.BEFORE || anchor == Anchor.AFTER) {
      "Only `Anchor.BEFORE` and `Anchor.AFTER` make sense for relative constraints"
    }
    return Constraints(anchor, actionId)
  }

  companion object {
    private val LOG = logger<InitializationListener>()

    const val RECENT_COURSES_FILLED = "Educational.recentCoursesFilled"
    const val STEPIK_AUTH_RESET = "Educational.stepikOAuthReset"
    private const val SWITCH_TO_COMMUNITY_DO_NOT_ASK_OPTION_ID = "Edu IDEs aren't supported"
  }
}