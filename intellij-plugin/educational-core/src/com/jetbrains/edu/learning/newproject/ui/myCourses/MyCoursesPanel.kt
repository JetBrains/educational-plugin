package com.jetbrains.edu.learning.newproject.ui.myCourses

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.jetbrains.edu.learning.actions.EduActionUtils
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.ToolbarActionWrapper
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import kotlinx.coroutines.CoroutineScope

private const val ACTION_PLACE = "MyCoursesPanel"

class MyCoursesPanel(
  myCoursesProvider: CoursesPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(myCoursesProvider, scope, disposable) {

  override fun toolbarAction(): ToolbarActionWrapper {
    coursesSearchComponent.hideFilters()
    val importCourseAction = EduActionUtils.getAction(ImportLocalCourseAction.ACTION_ID)
    return ToolbarActionWrapper(EduCoreBundle.lazyMessage("course.dialog.open.course.from.disk.lowercase"), importCourseAction)
  }

  override fun setNoCoursesPanelDefaultText(panel: JBPanelWithEmptyText) {
    val emptyText = panel.emptyText
    emptyText.text = EduCoreBundle.message("course.dialog.my.courses.no.courses.started")
    emptyText.appendSecondaryText(
      EduCoreBundle.message("course.dialog.open.course.from.disk"),
      SimpleTextAttributes.LINK_ATTRIBUTES,
      ActionUtil.createActionListener(ImportLocalCourseAction.ACTION_ID, this, ACTION_PLACE)
    )
  }

  override fun updateFilters(coursesGroups: List<CoursesGroup>) {
    super.updateFilters(coursesGroups)
    coursesSearchComponent.selectAllHumanLanguageItems()
  }

  override fun updateModelAfterCourseDeletedFromStorage(deletedCourse: JBACourseFromStorage) {
    coursesGroups.clear()
    coursesGroups.addAll(CoursesStorage.getInstance().coursesInGroups())
    super.updateModelAfterCourseDeletedFromStorage(deletedCourse)
  }

  override fun createCoursesListPanel() = MyCoursesList()

  inner class MyCoursesList : CoursesListWithResetFilters() {
    override fun createCourseCard(course: Course): CourseCardComponent {
      return MyCourseCardComponent(course)
    }
  }
}
