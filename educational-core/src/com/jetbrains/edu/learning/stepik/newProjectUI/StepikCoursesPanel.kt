package com.jetbrains.edu.learning.stepik.newProjectUI

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ui.Messages
import com.intellij.util.messages.MessageBusConnection
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.*
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikNames.STEPIK_HELP
import com.jetbrains.edu.learning.stepik.api.StepikCoursesProvider
import com.jetbrains.edu.learning.stepik.course.StartStepikCourseAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class StepikCoursesPanel(platformProvider: CoursesPlatformProvider,
                         private val coursesProvider: StepikCoursesProvider,
                         scope: CoroutineScope) : CoursesPanel(platformProvider, scope) {
  private var busConnection: MessageBusConnection? = null

  override fun toolbarAction(): ToolbarActionWrapper {
    return ToolbarActionWrapper(EduCoreBundle.lazyMessage("stepik.courses.open.by.link", StepikNames.STEPIK), OpenStepikCourseByLink())
  }

  override fun tabInfo(): TabInfo {
    val linkText = """<a href="$STEPIK_HELP">${StepikNames.STEPIK}</a>"""
    val infoText = EduCoreBundle.message("stepik.courses.explanation", linkText)
    val loginComponent = StepikLoginPanel()
    return TabInfo(infoText, loginComponent)
  }

  override suspend fun updateCoursesAfterLogin(preserveSelection: Boolean) {
    val privateCourses = withContext(Dispatchers.IO) { coursesProvider.loadPrivateCourseInfos() }
    if (privateCourses.isEmpty()) return
    coursesGroups.firstOrNull()?.name = EduCoreBundle.message("course.dialog.public.courses.group")
    val privateCoursesGroup = CoursesGroup(EduCoreBundle.message("course.dialog.private.courses.group"), privateCourses)
    coursesGroups.add(0, privateCoursesGroup)
    super.updateCoursesAfterLogin(preserveSelection)
  }

  private inner class StepikLoginPanel : LoginPanel(isLoginNeeded(),
                                                    EduCoreBundle.message("course.dialog.log.in.label.before.link"),
                                                    EduCoreBundle.message("course.dialog.log.in.to", StepikNames.STEPIK).toLowerCase(),
                                                    { handleLogin() })

  private fun handleLogin() {
    addLoginListener({ hideLoginPanel() }, { coursePanel.hideErrorPanel() })
    StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
    EduCounterUsageCollector.loggedIn(StepikNames.STEPIK, EduCounterUsageCollector.AuthorizationPlace.START_COURSE_DIALOG)
  }

  override fun isLoginNeeded(): Boolean = !EduSettings.isLoggedIn()

  private fun addLoginListener(vararg postLoginActions: () -> Unit) {
    if (busConnection != null) {
      busConnection!!.disconnect()
    }
    busConnection = ApplicationManager.getApplication().messageBus.connect()
    busConnection!!.subscribe(EduSettings.SETTINGS_CHANGED, object : EduLogInListener {
      override fun userLoggedOut() {}
      override fun userLoggedIn() {
        runPostLoginActions(*postLoginActions)
      }
    })
  }

  private fun runPostLoginActions(vararg postLoginActions: () -> Unit) {
    invokeLater(modalityState = ModalityState.any()) {
      for (action in postLoginActions) {
        action()
      }
      if (busConnection != null) {
        busConnection!!.disconnect()
        busConnection = null
      }
    }
  }

  override fun createCoursesListPanel() = StepikCoursesListPanel()

  inner class StepikCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return EduCourseCard(course)
    }
  }

  private inner class OpenStepikCourseByLink : AnAction() {
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
        EduCoreBundle.message("course.dialog.log.in.to.title", StepikNames.STEPIK),
        EduCoreBundle.message("stepik.log.in.dialog.ok"),
        EduCoreBundle.message("dialog.cancel"),
        null
      )

      if (result == Messages.OK) {
        addLoginListener(this@OpenStepikCourseByLink::importCourse)
        StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
      }
    }

    private fun importCourse() {
      val course = StartStepikCourseAction().importCourse() ?: return
      val coursesGroup = coursesGroups.first()
      val alreadyAddedCourse = coursesGroup.courses.find { it.id == course.id && it.languageID == course.languageID }
      if (alreadyAddedCourse != null) {
        updateModel(coursesGroups, alreadyAddedCourse, false)
      }
      else {
        coursesGroup.courses = coursesGroup.courses + course
        updateModel(coursesGroups, course, false)
      }
    }

    override fun displayTextInToolbar(): Boolean {
      return true
    }
  }
}