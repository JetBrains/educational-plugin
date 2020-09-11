package com.jetbrains.edu.learning.stepik.course

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.newproject.ui.ErrorComponent
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.ValidationMessageType
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import org.apache.commons.lang.math.NumberUtils.isDigits
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class ImportStepikCoursePanel(private val parent: Disposable) {
  private val courseLinkTextField = JTextField()
  val panel: JPanel
  private val helpLabel = JLabel("https://stepik.org/course/*")
  private val errorComponent = ErrorComponent(getHyperlinkListener())
  private var validationListener: ValidationListener? = null

  val courseLink: String
    get() = courseLinkTextField.text

  val preferredFocusedComponent: JComponent?
    get() = courseLinkTextField

  init {
    helpLabel.foreground = UIUtil.getLabelDisabledForeground()
    helpLabel.font = UIUtil.getLabelFont()
    errorComponent.setErrorMessage(ValidationMessage("",
                                                     "Log in",
                                                     " to Stepik to import course",
                                                     type = ValidationMessageType.ERROR))
    errorComponent.border = JBUI.Borders.empty(1)

    val courseLink = JLabel("Course link:")
    panel = JPanel(BorderLayout())
    val nestedPanel = JPanel()
    val layout = GroupLayout(nestedPanel)
    layout.autoCreateGaps = true
    nestedPanel.layout = layout
    layout.setHorizontalGroup(
      layout.createSequentialGroup()
        .addComponent(courseLink)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(courseLinkTextField)
                    .addComponent(helpLabel))
    )
    layout.setVerticalGroup(
      layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(courseLink)
                    .addComponent(courseLinkTextField))
        .addComponent(helpLabel)
    )
    panel.add(nestedPanel, BorderLayout.NORTH)
    panel.add(errorComponent, BorderLayout.SOUTH)
    panel.preferredSize = JBUI.size(Dimension(400, 90))
    panel.minimumSize = panel.preferredSize
  }

  fun setValidationListener(validationListener: ValidationListener?) {
    this.validationListener = validationListener
    doValidation()
  }

  private fun getHyperlinkListener(): HyperlinkListener = HyperlinkListener { e ->
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

  fun validate(): Boolean {
    val text = courseLinkTextField.text
    return text.isNotEmpty() && (isDigits(text) || isValidStepikLink(text))
  }

  private fun isValidStepikLink(text: String): Boolean {
    return StepikCourseConnector.getCourseIdFromLink(text) != -1
  }

  interface ValidationListener {
    fun onLoggedIn(isLoggedIn: Boolean)
  }
}