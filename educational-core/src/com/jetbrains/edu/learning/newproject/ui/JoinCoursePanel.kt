package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.ui.EduColors
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class JoinCoursePanel(private val settings: CoursePanel.CourseDisplaySettings) : JPanel(BorderLayout()) {

  private val myCoursePanel: CoursePanel = CoursePanel(true, true)
  private val myErrorLabel: JBLabel = JBLabel()

  private var myValidationListener: ValidationListener? = null

  init {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)

    myErrorLabel.border = JBUI.Borders.emptyTop(8)
    myErrorLabel.foreground = EduColors.errorTextForeground

    add(myCoursePanel, BorderLayout.CENTER)
    add(myErrorLabel, BorderLayout.SOUTH)

    setupValidation()
  }

  // '!!' is safe here because `myCoursePanel` has location field
  val locationString: String get() = myCoursePanel.locationString!!
  val projectSettings: Any get() = myCoursePanel.projectSettings

  fun bindCourse(course: Course) {
    myCoursePanel.bindCourse(course, settings).addSettingsChangeListener { doValidation(course) }
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
      locationString.isBlank() -> ErrorMessage("Enter course location")
      !FileUtil.ensureCanCreateFile(File(FileUtil.toSystemDependentName(locationString))) -> ErrorMessage("Can't create course at this location")
      else -> myCoursePanel.validateSettings(course, locationString)
    }
    updateErrorText(message)
    myValidationListener?.onInputDataValidated(message == null)
  }

  fun updateErrorText(message: ErrorMessage?) {
    myErrorLabel.text = if (message != null) message.beforeLink + message.linkText + message.afterLink else ""
    myErrorLabel.isVisible = message != null
  }

  interface ValidationListener {
    fun onInputDataValidated(isInputDataComplete: Boolean)
  }

  companion object {
    private const val WIDTH: Int = 370
    private const val HEIGHT: Int = 330
  }
}
