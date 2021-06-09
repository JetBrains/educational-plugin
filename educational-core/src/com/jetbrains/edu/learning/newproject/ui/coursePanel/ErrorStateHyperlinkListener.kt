package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.actions.SwitchTaskPanelAction
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.marketplace.installAndEnablePlugin
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.browseHyperlink
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import javax.swing.JTextPane
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class ErrorStateHyperlinkListener : HyperlinkListener {
  override fun hyperlinkUpdate(e: HyperlinkEvent?) {
    if (e?.eventType != HyperlinkEvent.EventType.ACTIVATED) return

    val coursePanel = UIUtil.getParentOfType(CoursePanel::class.java, e?.source as? JTextPane) ?: return
    val coursesPanel = UIUtil.getParentOfType(CoursesPanel::class.java, e?.source as? JTextPane) ?: return
    when (val state = coursePanel.errorState) {
      is ErrorState.CheckiOLoginRequired -> {
        val course = coursePanel.course as CheckiOCourse
        addCheckiOLoginListener(coursePanel, coursesPanel)

        //for Checkio course name matches platform name
        EduCounterUsageCollector.loggedIn(course.name, EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
      }
      is ErrorState.JetBrainsAcademyLoginNeeded -> {
        addLoginListener(coursePanel, coursesPanel)
        HyperskillConnector.getInstance().doAuthorize(
          Runnable { coursePanel.hideErrorPanel() },
          Runnable { coursesPanel.setButtonsEnabled(true) },
          Runnable { coursesPanel.hideLoginPanel() },
          Runnable { coursesPanel.scheduleUpdateAfterLogin() }
        )
      }
      is ErrorState.StepikLoginRequired, ErrorState.NotLoggedIn -> {
        addLoginListener(coursePanel, coursesPanel)
        // TODO: Update course list
        StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
        EduCounterUsageCollector.loggedIn(StepikNames.STEPIK, EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
      }
      ErrorState.JCEFRequired -> invokeSwitchUILibrary(coursePanel)
      ErrorState.IncompatibleVersion -> installAndEnablePlugin(setOf(PluginId.getId(EduNames.PLUGIN_ID))) {}
      is ErrorState.RequirePlugins -> {
        val pluginStringIds = state.pluginIds.mapTo(HashSet()) { it.id }
        installAndEnablePlugin(pluginStringIds) {}
      }
      ErrorState.RestartNeeded -> {
        DialogWrapper.findInstance(coursesPanel)?.close(DialogWrapper.OK_EXIT_CODE)
        ApplicationManager.getApplication().exit(true, true, true)
      }
      is ErrorState.CustomSevereError -> state.action?.run()
      else -> browseHyperlink(state.message)
    }
  }

  private fun addCheckiOLoginListener(coursePanel: CoursePanel, coursesPanel: CoursesPanel) {
    val course = coursePanel.course
    val checkiOConnectorProvider = (course?.configurator as CheckiOConnectorProvider?)!!
    val checkiOOAuthConnector = checkiOConnectorProvider.oAuthConnector
    checkiOOAuthConnector.doAuthorize(
      Runnable { coursesPanel.hideLoginPanel() },
      Runnable { coursePanel.hideErrorPanel() },
      Runnable { doValidation(coursePanel) }
    )
  }

  private fun addLoginListener(coursePanel: CoursePanel, coursesPanel: CoursesPanel) {
    val connection = ApplicationManager.getApplication().messageBus.connect()
    connection.subscribe(EduSettings.SETTINGS_CHANGED, object : EduLogInListener {
      override fun userLoggedOut() {}
      override fun userLoggedIn() {
        coursePanel.hideErrorPanel()
        coursesPanel.hideLoginPanel()
        doValidation(coursePanel)
        connection.disconnect()
        coursesPanel.scheduleUpdateAfterLogin()
      }
    })
  }

  private fun invokeSwitchUILibrary(coursePanel: CoursePanel) {
    val switchUILibraryAction = SwitchTaskPanelAction.ACTION_ID
    val action = ActionManager.getInstance().getAction(switchUILibraryAction)
    if (action == null) {
      Logger.getInstance(CoursesPanel::class.java).error("$switchUILibraryAction action not found")
      return
    }
    action.actionPerformed(
      AnActionEvent.createFromAnAction(
        action, null,
        ActionPlaces.UNKNOWN,
        DataManager.getInstance().getDataContext(coursePanel)
      )
    )
    doValidation(coursePanel)
  }

  private fun doValidation(coursePanel: CoursePanel) {
    var languageError: ErrorState = ErrorState.NothingSelected
    val course = coursePanel.course
    if (course != null) {
      val languageSettingsMessage = coursePanel.validateSettings(course)
      languageError = languageSettingsMessage?.let { ErrorState.LanguageSettingsError(it) } ?: ErrorState.None
    }
    val errorState = ErrorState.forCourse(course).merge(languageError)
    coursePanel.setError(errorState)
    coursePanel.setButtonsEnabled(errorState.courseCanBeStarted)
  }

}

