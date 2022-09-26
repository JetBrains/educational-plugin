package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.io.File
import javax.swing.JComponent

class CCCreateCourseArchiveDialog(project: Project, courseName: String) : DialogWrapper(project) {
  private val myPanel: CCCreateCourseArchivePanel

  init {
    title = EduCoreBundle.message("action.create.course.archive.text")
    myPanel = CCCreateCourseArchivePanel(project, courseName)
    myPreferredFocusedComponent = myPanel.locationField
    init()
  }

  override fun postponeValidation(): Boolean {
    return false
  }

  override fun doValidate(): ValidationInfo? {
    val file = File(locationPath)
    if (file.exists()) {
      return ValidationInfo(EduCoreBundle.message("course.creator.create.archive.invalid.location.file.exists"),
                            myPanel.locationField).asWarning().withOKEnabled()
    }
    if (!EduUtils.isZip(locationPath)) {
      return ValidationInfo(EduCoreBundle.message("course.creator.create.archive.invalid.location.wrong.extension"), myPanel.locationField)
    }
    return super.doValidate()
  }

  override fun createCenterPanel(): JComponent {
    return myPanel
  }

  val locationPath: String
    get() = myPanel.locationPath

  val authorName: String
    get() = myPanel.authorName

  val checkTasksFlag: Boolean
    get() = myPanel.checkAllTasksFlag
}