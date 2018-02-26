package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.jetbrains.edu.learning.courseFormat.Course
import java.awt.Dimension
import javax.swing.JComponent

open class CCEditCourseInfoDialog(val project: Project,
                                  val course: Course,
                                  private val dialogTitle: String) : DialogWrapper (project) {
  private val panel: CCCourseInfoPanel = CCCourseInfoPanel(course.name, Course.getAuthorsString(course.authors), course.description)

  init {
    super.init()
    initValidation()
  }

  fun setOkButtonText(text: String) {
    setOKButtonText(text)
  }

  fun showAndApply(): Boolean {
    setValidationDelay(0)
    if (showAndGet()) {
      course.setAuthorsAsString(panel.authors)
      course.name = panel.name
      course.description = panel.description
      setVersion(course, panel)
      ProjectView.getInstance(project).refresh()
      ProjectInspectionProfileManager.getInstance(project).fireProfileChanged()
      return true
    }
    return false
  }

  open fun setVersion(course: Course, panel: CCCourseInfoPanel) {}

  open fun setupLanguageLevels(course: Course, panel: CCCourseInfoPanel) {}

  override fun doValidate(): ValidationInfo? {
    return panel.validate()
  }

  override fun createCenterPanel(): JComponent? {
    title = this.dialogTitle
    setupLanguageLevels(course, panel)


    val mainPanel = panel.mainPanel
    mainPanel.preferredSize = Dimension(450, 300)
    mainPanel.minimumSize = Dimension(450, 300)

    return mainPanel
  }

  fun showAuthor(isVisible: Boolean) {
    panel.showAuthorField(isVisible)
  }
}