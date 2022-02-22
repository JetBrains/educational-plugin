package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.actions.StartCodeforcesContestAction
import com.jetbrains.edu.learning.codeforces.authorization.LoginDialog
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.LoginPanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import javax.swing.JButton

class CodeforcesCoursesPanel(
  platformProvider: CoursesPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {

  init {
    coursesSearchComponent.hideFilters()
    coursesSearchComponent.add(createOpenContestButtonPanel(), BorderLayout.LINE_END)
  }

  override fun createCoursePanel(disposable: Disposable): CoursePanel {
    return CodeforcesCoursePanel(disposable)
  }

  private fun createOpenContestButtonPanel(): NonOpaquePanel {
    val button = JButton(EduCoreBundle.message("codeforces.open.contest.by.link")).apply {
      background = MAIN_BG_COLOR
      isOpaque = false
      addActionListener(ActionUtil.createActionListener(StartCodeforcesContestAction.ACTION_ID, this, PLACE))
    }

    return NonOpaquePanel().apply {
      border = JBUI.Borders.empty(0, 0, 0, 12)
      add(button)
    }
  }

  override fun getEmptySearchText(): String {
    return EduCoreBundle.message("codeforces.search.placeholder")
  }

  override fun tabDescription(): String {
    val linkText = """<a href="${CodeforcesNames.CODEFORCES_URL}">${CodeforcesNames.CODEFORCES_TITLE}</a>"""
    return EduCoreBundle.message("codeforces.courses.description", linkText)
  }

  override fun createCoursesListPanel(): CoursesListWithResetFilters {
    return CodeforcesCoursesListPanel()
  }

  override fun getLoginComponent(): LoginPanel {
    return CodeforcesLoginPanel()
  }

  private inner class CodeforcesLoginPanel : LoginPanel(isLoginNeeded(),
                                                        CodeforcesNames.CODEFORCES_TITLE,
                                                        EduCoreBundle.message("codeforces.login.to.codeforces.message"),
                                                        { handleLogin() })

  private fun handleLogin() {
      if (LoginDialog().showAndGet() && CodeforcesSettings.getInstance().isLoggedIn()) {
        hideLoginPanel()
      }
  }

  override fun isLoginNeeded(): Boolean = !CodeforcesSettings.getInstance().isLoggedIn()

  private inner class CodeforcesCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCourseCard(course: Course): CourseCardComponent {
      val courseMetaInfo = CoursesStorage.getInstance().getCourseMetaInfoForAnyLanguage(course)
      if (courseMetaInfo != null) {
        course.programmingLanguage = courseMetaInfo.programmingLanguage
      }
      return CodeforcesCardComponent(course)
    }
  }

  companion object {
    @NonNls
    const val PLACE = "Codeforces Courses Panel"
  }
}
