package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.jetbrains.edu.learning.messages.EduCoreActionBundle
import java.io.File
import javax.swing.JComponent

class CCCreateCourseArchiveDialog(project: Project, courseName: String, showAuthorField: Boolean) : DialogWrapper(project) {
  private val myPanel: CCCreateCourseArchivePanel

  init {
    title = EduCoreActionBundle.message("action.create.course.archive.text")
    myPanel = CCCreateCourseArchivePanel(project, courseName, showAuthorField)
    addPanelListener()
    init()
  }

  private fun addPanelListener() {
    myPanel.addLocationListener {
      val location = locationPath
      val file = File(location)
      if (!file.exists() || !file.isDirectory) {
        myOKAction.isEnabled = false
        myPanel.setError()
      }
      myOKAction.isEnabled = true
    }
  }

  override fun createCenterPanel(): JComponent? {
    return myPanel
  }

  val zipName: String
    get() = myPanel.zipName

  val locationPath: String
    get() = myPanel.locationPath

  val authorName: String
    get() = myPanel.authorName
}