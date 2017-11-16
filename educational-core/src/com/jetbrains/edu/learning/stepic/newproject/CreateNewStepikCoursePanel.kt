package com.jetbrains.edu.learning.stepic.newproject

import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.CoursePanel
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

class CreateNewStepikCoursePanel : JPanel(BorderLayout()) {

  private val myCoursePanel: CoursePanel = CoursePanel(true, true)
  private val myErrorLabel: JBLabel = JBLabel()

  private var myValidationListener: ValidationListener? = null

  init {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)

    myErrorLabel.border = JBUI.Borders.emptyTop(8)
    myErrorLabel.foreground = MessageType.ERROR.titleForeground

    add(myCoursePanel, BorderLayout.CENTER)
    add(myErrorLabel, BorderLayout.SOUTH)

    setupValidation()
  }

  // '!!' is safe here because `myCoursePanel` has location field
  val locationString: String get() = myCoursePanel.locationString!!
  val projectSettings: Any get() = myCoursePanel.projectSettings

  fun bindCourse(course: Course) {
    myCoursePanel.bindCourse(course)
  }

  fun setValidationListener(listener: ValidationListener?) {
    myValidationListener = listener
    doValidation()
  }

  private fun setupValidation() {
    val validator = object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        doValidation()
      }
    }
    myCoursePanel.addLocationFieldDocumentListener(validator)
  }

  private fun doValidation() {
    val message = when {
      locationString.isBlank() -> "Enter course location"
      !FileUtil.ensureCanCreateFile(File(FileUtil.toSystemDependentName(locationString))) -> "Can't create course at this location"
      else -> null
    }
    myErrorLabel.text = message
    myErrorLabel.isVisible = message != null
    myValidationListener?.onInputDataValidated(message == null)
  }

  interface ValidationListener {
    fun onInputDataValidated(isInputDataComplete: Boolean)
  }

  companion object {
    private const val WIDTH: Int = 370
    private const val HEIGHT: Int = 330
  }
}
