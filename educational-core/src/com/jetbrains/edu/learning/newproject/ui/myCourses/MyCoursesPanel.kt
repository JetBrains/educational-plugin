package com.jetbrains.edu.learning.newproject.ui.myCourses

import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.jetbrains.edu.learning.actions.ImportLocalCourseAction
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseDeletedListener
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.ToolbarActionWrapper
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import kotlinx.coroutines.CoroutineScope
import java.awt.event.ActionListener
import javax.swing.JPanel

private const val ACTION_PLACE = "MyCoursesPanel"

class MyCoursesPanel(
  myCoursesProvider: CoursesPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(myCoursesProvider, scope, true) {

  init {
    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(CoursesStorage.COURSE_DELETED, object : CourseDeletedListener {
      override fun courseDeleted(course: Course) {
        coursesGroups.clear()
        coursesGroups.addAll(CoursesStorage.getInstance().coursesInGroups())
        onTabSelection()
      }
    })
  }

  override fun toolbarAction(): ToolbarActionWrapper {
    return ToolbarActionWrapper(EduCoreBundle.lazyMessage("course.dialog.open.course.from.disk.lowercase"), ImportLocalCourseAction())
  }

  override fun createNoCoursesPanel(): JPanel {
    return JBPanelWithEmptyText().apply {
      emptyText.text = EduCoreBundle.message("course.dialog.my.courses.no.courses.started")
      emptyText.appendSecondaryText(
        EduCoreBundle.message("course.dialog.open.course.from.disk"),
        SimpleTextAttributes.LINK_ATTRIBUTES,
        ActionListener {
          val action = ImportLocalCourseAction(EduCoreBundle.lazyMessage("course.dialog.open.course.from.disk.ellipsis"))
          val dataContext = DataManager.getInstance().getDataContext(this)
          val actionEvent = AnActionEvent.createFromAnAction(action, null, ACTION_PLACE, dataContext)
          action.actionPerformed(actionEvent)
        }
      )
    }
  }

  override fun updateFilters(coursesGroups: List<CoursesGroup>) {
    super.updateFilters(coursesGroups)
    humanLanguagesFilterDropdown.selectedItems = humanLanguagesFilterDropdown.allItems
  }
}
