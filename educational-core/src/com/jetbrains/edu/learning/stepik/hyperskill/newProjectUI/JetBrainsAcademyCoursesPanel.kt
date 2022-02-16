package com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseMetaInfo
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.LoginPanel
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationPlace
import com.jetbrains.edu.learning.stepik.hyperskill.JBA_HELP
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class JetBrainsAcademyCoursesPanel(
  private val platformProvider: JetBrainsAcademyPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {

  override fun tabDescription(): String {
    val linkText = """<a href="${JBA_HELP}">${EduNames.JBA}</a>"""
    return EduCoreBundle.message("hyperskill.courses.explanation", linkText)
  }

  override fun updateModelAfterCourseDeletedFromStorage(deletedCourse: Course) {
    if (deletedCourse is CourseMetaInfo) {
      if (coursesGroups.isNotEmpty()) {
        val coursesGroup = coursesGroups.first()

        coursesGroup.courses = coursesGroup.courses.filter { it != deletedCourse }

        if (coursesGroup.courses.isEmpty()) {
          coursesGroup.courses = listOf(JetBrainsAcademyCourse())
        }
      }
    }

    super.updateModelAfterCourseDeletedFromStorage(deletedCourse)
  }

  override fun createCoursesListPanel() = JetBrainsAcademyCoursesListPanel()

  override fun getLoginComponent(disposable: Disposable): LoginPanel = JetBrainsAcademyLoginPanel(disposable)

  inner class JetBrainsAcademyCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return JetBrainsAcademyCourseCard(course)
    }
  }

  private inner class JetBrainsAcademyLoginPanel(disposable: Disposable) : LoginPanel(
    isLoginNeeded(),
    EduNames.JBA,
    EduCoreBundle.message("course.dialog.jba.log.in.label.before.link")
  ) {
    init {
      HyperskillConnector.getInstance().subscribe(object : EduLogInListener {
        override fun userLoggedIn() {
          runInEdt(ModalityState.any()) {
            coursePanel.hideErrorPanel()
            setButtonsEnabled(true)
            hideLoginPanel()
            scheduleUpdateAfterLogin()
          }
        }
      }, disposable)
    }

    override fun handleLogin() {
      HyperskillConnector.getInstance().doAuthorize(authorizationPlace = AuthorizationPlace.START_COURSE_DIALOG)
    }
  }

  override fun isLoginNeeded() = HyperskillSettings.INSTANCE.account == null

  override suspend fun updateCoursesAfterLogin(preserveSelection: Boolean) {
    val academyCoursesGroups = withContext(Dispatchers.IO) { platformProvider.loadCourses() }
    coursesGroups.clear()
    coursesGroups.addAll(academyCoursesGroups)
    super.updateCoursesAfterLogin(false)
  }
}