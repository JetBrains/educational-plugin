package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ide.DataManager
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.ide.plugins.PluginStateManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.actions.SwitchTaskPanelAction
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.marketplace.installAndEnablePlugin
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.browseHyperlink
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import javax.swing.JTextPane
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class ErrorStateHyperlinkListener(private val parentDisposable: Disposable) : HyperlinkListener {
  override fun hyperlinkUpdate(e: HyperlinkEvent?) {
    if (e?.eventType != HyperlinkEvent.EventType.ACTIVATED) return

    val coursePanel = UIUtil.getParentOfType(CoursePanel::class.java, e?.source as? JTextPane) ?: return
    val coursesPanel = UIUtil.getParentOfType(CoursesPanel::class.java, e?.source as? JTextPane)
    val logInListener = object : EduLogInListener {
      override fun userLoggedIn() {
        runInEdt(ModalityState.any()) {
          coursePanel.hideErrorPanel()
          coursesPanel?.hideLoginPanel()
          doValidation(coursePanel)
          coursesPanel?.scheduleUpdateAfterLogin()
        }
      }
    }

    when (val state = coursePanel.errorState) {
      is ErrorState.CheckiOLoginRequired -> {
        val checkiOConnectorProvider = (coursePanel.course?.configurator as CheckiOConnectorProvider?)
        if (checkiOConnectorProvider == null) {
          Logger.getInstance(CoursesPanel::class.java).error("CheckiO connector provider is not found")
          return
        }
        checkiOConnectorProvider.oAuthConnector.apply {
          subscribe(logInListener, parentDisposable)
          doAuthorize(authorizationPlace = AuthorizationPlace.START_COURSE_DIALOG)
        }
      }
      is ErrorState.JetBrainsAcademyLoginNeeded -> {
        HyperskillConnector.getInstance().apply {
          subscribe(logInListener, parentDisposable)
          doAuthorize(authorizationPlace = AuthorizationPlace.START_COURSE_DIALOG)
        }
      }
      is ErrorState.StepikLoginRequired, ErrorState.NotLoggedIn -> {
        // TODO: Update course list
        StepikConnector.getInstance().apply {
          subscribe(logInListener, parentDisposable)
          doAuthorize(authorizationPlace = AuthorizationPlace.START_COURSE_DIALOG)
        }
      }
      is ErrorState.JCEFRequired -> invokeSwitchUILibrary(coursePanel)
      is ErrorState.IncompatibleVersion -> installAndEnablePlugin(setOf(PluginId.getId(EduNames.PLUGIN_ID))) {}
      is ErrorState.RequirePlugins -> {
        val listener = object : PluginStateListener {
          override fun install(descriptor: IdeaPluginDescriptor) {
            coursePanel.doValidation()
          }

          override fun uninstall(descriptor: IdeaPluginDescriptor) {

          }
        }

        Disposer.register(parentDisposable, Disposable {
          PluginStateManager.removeStateListener(listener)
        })
        val pluginStringIds = state.pluginIds.mapTo(HashSet()) { it.id }
        PluginStateManager.addStateListener(listener)
        installAndEnablePlugin(pluginStringIds) {}
      }
      is ErrorState.RestartNeeded -> {
        //close Course Selection View if it's open
        DialogWrapper.findInstance(coursesPanel)?.close(DialogWrapper.OK_EXIT_CODE)

        //close individual Start Course dialog if it's open
        DialogWrapper.findInstance(coursePanel)?.close(DialogWrapper.OK_EXIT_CODE)

        ApplicationManager.getApplication().exit(true, true, true)
      }
      is ErrorState.CustomSevereError -> state.action?.run()
      else -> browseHyperlink(state.message)
    }
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

