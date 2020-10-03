package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class CCCreateCourseArchiveDialog(project: Project, courseName: String, showAuthorField: Boolean) : DialogWrapper(project) {
  private val myPanel: CCCreateCourseArchivePanel

  init {
    title = EduCoreBundle.message("action.create.course.archive.text")
    myPanel = CCCreateCourseArchivePanel(project, courseName, showAuthorField)
    addPanelListener()
    validateLocation()
    init()
  }

  private fun addPanelListener() {
    myPanel.addLocationListener(object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent) {
        validateLocation()
      }

      override fun removeUpdate(e: DocumentEvent) {
        validateLocation()
      }

      override fun changedUpdate(e: DocumentEvent) {
        validateLocation()
      }
    })
  }

  private fun validateLocation() {
    myPanel.setErrorVisible(false)
    if (!EduUtils.isZip(locationPath)) {
      isOKActionEnabled = false
      myPanel.setError()
      return
    }
    isOKActionEnabled = true
  }

  override fun createCenterPanel(): JComponent? {
    return myPanel
  }

  val locationPath: String
    get() = myPanel.locationPath

  val authorName: String
    get() = myPanel.authorName
}