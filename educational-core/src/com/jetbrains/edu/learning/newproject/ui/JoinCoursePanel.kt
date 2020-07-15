package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.HyperlinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDisplaySettings
import com.jetbrains.edu.learning.newproject.ui.coursePanel.NewCoursePanel
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.ui.EduColors
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class JoinCoursePanel(private val settings: CourseDisplaySettings) : JPanel(BorderLayout()) {

  private val myCoursePanel: NewCoursePanel = NewCoursePanel(isStandalonePanel = true, isLocationFieldNeeded = true) {
    setError(it.message)
  }
  private val myErrorLabel: HyperlinkLabel = HyperlinkLabel()
  private var myValidationMessage: ValidationMessage? = null
  private var myValidationListener: ValidationListener? = null

  init {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)

    myErrorLabel.border = JBUI.Borders.emptyTop(8)
    myErrorLabel.foreground = EduColors.errorTextForeground
    myErrorLabel.addHyperlinkListener { browseHyperlink(myValidationMessage) }
    add(myCoursePanel, BorderLayout.CENTER)
    add(myErrorLabel, BorderLayout.SOUTH)
    UIUtil.setBackgroundRecursively(this, TaskDescriptionView.getTaskDescriptionBackgroundColor())

    setupValidation()
  }

  // '!!' is safe here because `myCoursePanel` has location field
  val locationString: String get() = myCoursePanel.locationString!!

  val languageSettings: LanguageSettings<*>? get() = myCoursePanel.languageSettings

  fun bindCourse(course: Course) {
    myCoursePanel.bindCourse(course, settings)?.addSettingsChangeListener { doValidation(course) }
  }

  fun setValidationListener(course: Course, listener: ValidationListener?) {
    myValidationListener = listener
    doValidation(course)
  }

  private fun setupValidation() {
    val validator = object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        doValidation(null)
      }
    }
    myCoursePanel.addLocationFieldDocumentListener(validator)
  }

  private fun doValidation(course: Course?) {
    val message = when {
      course != null && course.configurator == null -> ValidationMessage(course.unsupportedCourseMessage)
      locationString.isBlank() -> ValidationMessage("Enter course location")
      !FileUtil.ensureCanCreateFile(File(FileUtil.toSystemDependentName(locationString))) ->
        ValidationMessage("Can't create course at this location")
      else -> myCoursePanel.validateSettings(course)
    }
    setError(message)
    myValidationListener?.onInputDataValidated(message == null || message.type != ValidationMessageType.ERROR)
  }

  private fun setError(message: ValidationMessage?) {
    myValidationMessage = message
    updateErrorText(message)
  }

  fun updateErrorText(message: ValidationMessage?) {
    // myErrorLabel text may be too long and not fit on the JoinCoursePanel.
    // For JLabel text we can use HTML tags to automatically wrap text to available space
    if (message != null) {
      myErrorLabel.setHyperlinkText(message.beforeLink, message.linkText, message.afterLink)
    }
    myErrorLabel.isVisible = message != null
  }

  interface ValidationListener {
    fun onInputDataValidated(isInputDataComplete: Boolean)
  }

  companion object {
    private const val WIDTH: Int = 500
    private const val HEIGHT: Int = 570
  }
}
