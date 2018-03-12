package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.CoursePanel
import javax.swing.JComponent

open class CCEditCourseInfoDialog(val project: Project,
                                  val course: Course,
                                  dialogTitle: String) : DialogWrapper (project) {
  private val myPanel: CoursePanel = CoursePanel(true, false, true).apply {
    preferredSize = JBUI.size(WIDTH, HEIGHT)
    minimumSize = JBUI.size(WIDTH, HEIGHT)
  }

  init {
    super.init()
    title = dialogTitle
    myPanel.bindCourse(course)
    myPanel.addValidationListener { isInputDataComplete -> isOKActionEnabled = isInputDataComplete }
    this.setupLanguageLevels(course, myPanel)
    initValidation()
  }

  fun setOkButtonText(text: String) {
    setOKButtonText(text)
  }

  fun showAndApply(): Boolean {
    setValidationDelay(0)
    if (showAndGet()) {
      myPanel.applyChanges(course)
      setVersion(course, myPanel)
      ProjectView.getInstance(project).refresh()
      ProjectInspectionProfileManager.getInstance(project).fireProfileChanged()
      return true
    }
    return false
  }

  fun showAuthor(isToShow: Boolean) {
    myPanel.isAuthorEditable(isToShow)
  }

  open fun setVersion(course: Course, panel: CoursePanel) {}

  open fun setupLanguageLevels(course: Course, panel: CoursePanel) {}

  override fun createCenterPanel(): JComponent? {
    return myPanel
  }

  companion object {

    private const val WIDTH: Int = 370
    private const val HEIGHT: Int = 400
  }
}