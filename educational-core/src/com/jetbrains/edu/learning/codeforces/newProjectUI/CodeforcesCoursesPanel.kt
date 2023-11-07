package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.actions.StartCodeforcesContestAction
import com.jetbrains.edu.learning.codeforces.authorization.LoginDialog
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODEFORCES_URL
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CoursesDownloadingException
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.coursesList.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.platformProviders.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.coursesList.LoginPanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
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
      background = SelectCourseBackgroundColor
      isOpaque = false
      addActionListener(ActionUtil.createActionListener(StartCodeforcesContestAction.ACTION_ID, this, PLACE))
    }

    return NonOpaquePanel().apply {
      border = JBUI.Borders.emptyRight(12)
      add(button)
    }
  }

  override fun getEmptySearchText(): String {
    return EduCoreBundle.message("codeforces.search.placeholder")
  }

  override fun tabDescription(): String {
    val linkText = """<a href="${CodeforcesNames.CODEFORCES_HELP}">${CodeforcesNames.CODEFORCES_TITLE}</a>"""
    return EduCoreBundle.message("codeforces.courses.description", linkText)
  }

  override fun createCoursesListPanel(): CoursesListWithResetFilters {
    return CodeforcesCoursesListPanel()
  }

  override fun getLoginComponent(): LoginPanel {
    return CodeforcesLoginPanel()
  }

  private inner class CodeforcesLoginPanel : LoginPanel(EduCoreBundle.message("course.dialog.log.in.to.codeforces.label.text"),
                                                        isLoginNeeded(),
                                                        { handleLogin() })

  private fun handleLogin() {
      if (LoginDialog(AuthorizationPlace.START_COURSE_DIALOG).showAndGet() && CodeforcesSettings.getInstance().isLoggedIn()) {
        hideLoginPanel()
      }
  }

  override fun isLoginNeeded(): Boolean = !CodeforcesSettings.getInstance().isLoggedIn()

  override fun showErrorMessage(e: CoursesDownloadingException) {
    val text = noCoursesPanel.emptyText
    text.text = EduCoreBundle.message("codeforces.anti.crawler.start")
    text.appendSecondaryText(EduCoreBundle.message("codeforces.anti.crawler.end2") + " ", SimpleTextAttributes.GRAYED_ATTRIBUTES, null)
    text.appendSecondaryText(EduCoreBundle.message("action.open.on.text", CodeforcesNames.CODEFORCES_TITLE),
                             SimpleTextAttributes.LINK_ATTRIBUTES) { EduBrowser.getInstance().browse(CODEFORCES_URL) }
    text.appendLine(EduCoreBundle.message("help.use.guide1") + " ", SimpleTextAttributes.GRAYED_ATTRIBUTES, null)
    @Suppress("DialogTitleCapitalization") // it's ok to start from lowercase as it's the second part of a sentence
    text.appendText(EduCoreBundle.message("help.use.guide2"),
                    SimpleTextAttributes.LINK_ATTRIBUTES) { EduBrowser.getInstance().browse(EduNames.CODEFORCES_ANTI_CRAWLER_URL) }
  }

  private inner class CodeforcesCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCourseCard(course: Course): CourseCardComponent {
      val courseMetaInfo = CoursesStorage.getInstance().getCourseMetaInfoForAnyLanguage(course)
      if (courseMetaInfo != null) {
        course.languageId = courseMetaInfo.languageId
        course.languageVersion = courseMetaInfo.languageVersion
      }
      return CodeforcesCardComponent(course)
    }
  }

  companion object {
    @NonNls
    const val PLACE = "Codeforces Courses Panel"
  }
}
