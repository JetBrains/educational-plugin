package com.jetbrains.edu.learning.stepik.newProjectUI

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.*
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StartStepikCourseAction


class StepikCoursesPanel(dialog: BrowseCoursesDialog, platformProvider: CoursesPlatformProvider) : CoursesPanel(dialog, platformProvider) {
  override fun toolbarAction(): AnAction? {
    return OpenStepikCourseByLink()
  }

  override fun tabInfo(): TabInfo? {
    val infoText = EduCoreBundle.message("stepik.courses.explanation", StepikNames.STEPIK)
    val linkText = EduCoreBundle.message("course.dialog.go.to.website")
    val linkInfo = LinkInfo(linkText, StepikNames.STEPIK_URL)
    val loginComponent = StepikLoginPanel()
    return TabInfo(infoText, linkInfo, loginComponent)
  }

  private inner class StepikLoginPanel : LoginPanel(!EduSettings.isLoggedIn(),
                                                    EduCoreBundle.message("course.dialog.log.in.label.before.link"),
                                                    EduCoreBundle.message("course.dialog.log.in.to", StepikNames.STEPIK).toLowerCase(),
                                                    { handleLogin() })

  private fun handleLogin() {
    coursesListPanel.addLoginListener({ coursesListPanel.hideLoginPanel() }, { coursePanel.hideErrorPanel() })
    StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
    EduCounterUsageCollector.loggedIn(StepikNames.STEPIK, EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
  }


  private inner class OpenStepikCourseByLink : AnAction(EduCoreBundle.message("stepik.courses.open.by.link", StepikNames.STEPIK), null,
                                                        null) {
    override fun actionPerformed(e: AnActionEvent) {
      if (EduSettings.isLoggedIn()) {
        importCourse()
      }
      else {
        showLogInDialog()
      }
    }

    private fun showLogInDialog() {
      val result = Messages.showOkCancelDialog(
        EduCoreBundle.message("stepik.auth.required.message", StepikNames.STEPIK),
        EduCoreBundle.message("course.dialog.log.in.to", StepikNames.STEPIK),
        EduCoreBundle.message("stepik.log.in.dialog.ok"),
        EduCoreBundle.message("stepik.log.in.dialog.cancel"),
        null
      )

      if (result == Messages.OK) {
        coursesListPanel.addLoginListener({ importCourse() })
        StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
      }
    }

    private fun importCourse() {
      val course = StartStepikCourseAction().importStepikCourse() ?: return
      val courses = coursesListPanel.courses
      updateModel(courses.plus(course), course, false)
    }

    override fun displayTextInToolbar(): Boolean {
      return true
    }
  }
}