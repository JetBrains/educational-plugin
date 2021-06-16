package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.StartCodeforcesContestAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.TabInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import kotlinx.coroutines.CoroutineScope
import java.awt.BorderLayout
import javax.swing.JButton

class CodeforcesCoursesPanel(platformProvider: CoursesPlatformProvider, scope: CoroutineScope) : CoursesPanel(platformProvider, scope) {

  init {
    humanLanguagesFilterDropdown.isVisible = false
    programmingLanguagesFilterDropdown.isVisible = false

    searchPanel.add(createOpenContestButtonPanel(), BorderLayout.LINE_END)
  }

  private fun createOpenContestButtonPanel(): NonOpaquePanel {
    val button = JButton(EduCoreBundle.message("codeforces.open.contest.by.link")).apply {
      background = MAIN_BG_COLOR
      isOpaque = false
      addActionListener {
        val action = StartCodeforcesContestAction(showViewAllLabel = false)
        val anActionEvent = AnActionEvent(null,
                                          DataManager.getInstance().getDataContext(this),
                                          "Codeforces Courses Panel",
                                          action.templatePresentation,
                                          ActionManager.getInstance(), 0)
        action.actionPerformed(anActionEvent)
      }
    }

    return NonOpaquePanel().apply {
      border = JBUI.Borders.empty(0, 0, 0, 12)
      add(button)
    }
  }

  override fun tabInfo(): TabInfo {
    val linkText = """<a href="${CodeforcesNames.CODEFORCES_URL}">${CodeforcesNames.CODEFORCES_TITLE}</a>"""
    return TabInfo(EduCoreBundle.message("codeforces.courses.description", linkText))
  }

  override fun createCoursesListPanel(): CoursesListWithResetFilters {
    return CodeforcesCoursesListPanel()
  }

  private inner class CodeforcesCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCourseCard(course: Course): CourseCardComponent {
      val courseMetaInfo = CoursesStorage.getInstance().getCourseMetaInfoForAnyLanguage(course)
      if (courseMetaInfo != null) {
        course.language = courseMetaInfo.language
      }
      return CodeforcesCardComponent(course)
    }
  }
}