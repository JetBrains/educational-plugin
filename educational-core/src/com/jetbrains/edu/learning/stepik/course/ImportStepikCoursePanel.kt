package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.newproject.ui.ErrorComponent
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.ValidationMessageType
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class ImportStepikCoursePanel(
  courseConnector: CourseConnector,
  private val parent: Disposable
) : ImportCoursePanel(courseConnector, "https://stepik.org/course/*") {

  private var validationListener: ValidationListener? = null
  private val errorComponent = ErrorComponent(getHyperlinkListener()) { doValidation() }

  init {
    errorComponent.setErrorMessage(ValidationMessage("",
                                                     "Log in",
                                                     " to Stepik to import course",
                                                     type = ValidationMessageType.ERROR))
    errorComponent.border = JBUI.Borders.empty(1)
    panel.add(errorComponent, BorderLayout.SOUTH)
  }

  override fun setValidationListener(validationListener: ValidationListener?) {
    this.validationListener = validationListener
    doValidation()
  }

  fun getHyperlinkListener(): HyperlinkListener = HyperlinkListener { e ->
    if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
      if (!EduSettings.isLoggedIn()) {
        addLoginListener()
        StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
        EduCounterUsageCollector.loggedIn(StepikNames.STEPIK, EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
      }
    }
  }

  private fun addLoginListener() {
    val busConnection = ApplicationManager.getApplication().messageBus.connect(parent)
    busConnection.subscribe(EduSettings.SETTINGS_CHANGED, object : EduLogInListener {
      override fun userLoggedIn() {
        busConnection.disconnect()
        ApplicationManager.getApplication().invokeLater({ doValidation() }, ModalityState.any())
      }

      override fun userLoggedOut() {}
    })
  }

  private fun doValidation() {
    val isLoggedIn = EduSettings.isLoggedIn()
    errorComponent.isVisible = !isLoggedIn
    if (isLoggedIn) {
      // If user is logged - make panel smaller without `Log In` message
      setPanelSize(Dimension(400, 50))
    }
    validationListener?.onLoggedIn(isLoggedIn)
  }

  private fun setPanelSize(dimension: Dimension, isMinimumSizeEqualsPreferred: Boolean = true) {
    panel.preferredSize = JBUI.size(dimension)
    if (isMinimumSizeEqualsPreferred) {
      panel.minimumSize = panel.preferredSize
    }
  }
}