package com.jetbrains.edu.learning.stepik.newProjectUI

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ui.Messages
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.*
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StartStepikCourseAction
import kotlinx.coroutines.CoroutineScope


class StepikCoursesPanel(platformProvider: CoursesPlatformProvider, scope: CoroutineScope) : CoursesPanel(platformProvider, scope) {
  private var busConnection: MessageBusConnection? = null
  override fun toolbarAction(): AnAction? {
    return OpenStepikCourseByLink()
  }

  override fun tabInfo(): TabInfo? {
    val infoText = EduCoreBundle.message("stepik.courses.explanation", StepikNames.STEPIK)
    val linkText = EduCoreBundle.message("course.dialog.go.to.website")
    val linkInfo = LinkInfo(linkText, StepikNames.getStepikUrl())
    val loginComponent = StepikLoginPanel()
    return TabInfo(infoText, linkInfo, loginComponent)
  }

  private inner class StepikLoginPanel : LoginPanel(!EduSettings.isLoggedIn(),
                                                    EduCoreBundle.message("course.dialog.log.in.label.before.link"),
                                                    EduCoreBundle.message("course.dialog.log.in.to", StepikNames.STEPIK).toLowerCase(),
                                                    { handleLogin() })

  private fun handleLogin() {
    addLoginListener({ hideLoginPanel() }, { coursePanel.hideErrorPanel() })
    StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
    EduCounterUsageCollector.loggedIn(StepikNames.STEPIK, EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
  }

  private fun addLoginListener(vararg postLoginActions: () -> Unit) {
    if (busConnection != null) {
      busConnection!!.disconnect()
    }
    busConnection = ApplicationManager.getApplication().messageBus.connect()
    busConnection!!.subscribe(EduSettings.SETTINGS_CHANGED, object : EduLogInListener {
      override fun userLoggedOut() {}
      override fun userLoggedIn() {
        runPostLoginActions(*postLoginActions)
      }
    })
  }

  private fun runPostLoginActions(vararg postLoginActions: () -> Unit) {
    invokeLater(modalityState = ModalityState.any()) {
      for (action in postLoginActions) {
        action()
      }
      if (busConnection != null) {
        busConnection!!.disconnect()
        busConnection = null
      }
    }
  }

  private inner class OpenStepikCourseByLink : AnAction(EduCoreBundle.message("stepik.courses.open.by.link", StepikNames.STEPIK), null,
                                                        null) {
    override fun actionPerformed(e: AnActionEvent) {
      if (EduSettings.isLoggedIn()) {
        importCourse()
      }
      else {
        showLogInDialog()
      }
    }

    private fun showLogInDialog() {
      val result = Messages.showOkCancelDialog(
        EduCoreBundle.message("stepik.auth.required.message", StepikNames.STEPIK),
        EduCoreBundle.message("course.dialog.log.in.to", StepikNames.STEPIK),
        EduCoreBundle.message("stepik.log.in.dialog.ok"),
        EduCoreBundle.message("stepik.log.in.dialog.cancel"),
        null
      )

      if (result == Messages.OK) {
        addLoginListener(this@OpenStepikCourseByLink::importCourse)
        StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
      }
    }

    private fun importCourse() {
      val course = StartStepikCourseAction().importStepikCourse() ?: return
      val courses = courses
      updateModel(courses.plus(course), course, false)
    }

    override fun displayTextInToolbar(): Boolean {
      return true
    }
  }
}