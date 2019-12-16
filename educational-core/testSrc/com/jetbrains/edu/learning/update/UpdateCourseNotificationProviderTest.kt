package com.jetbrains.edu.learning.update

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils.getFirstTask
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.placeholderDependencies.NotificationsTestBase
import com.jetbrains.edu.learning.stepik.UpdateCourseNotificationProvider

class UpdateCourseNotificationProviderTest : NotificationsTestBase() {

  fun `test update course notification shown`() {
    val course = createCourse(isUpToDate = false)
    val notificationPanel = getNotificationPanel(course)
    assertNotNull("Notification not shown", notificationPanel)
  }

  fun `test update course notification not shown`() {
    val course = createCourse(isUpToDate = true)
    val notificationPanel = getNotificationPanel(course)
    assertNull("Notification is shown", notificationPanel)
  }

  private fun getNotificationPanel(course: EduCourse): EditorNotificationPanel? {
    val virtualFile = getFirstTask(course)!!.getTaskFile("Task.txt")!!.getVirtualFile(project)!!
    myFixture.openFileInEditor(virtualFile)

    completeEditorNotificationAsyncTasks()
    val fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(virtualFile)!!
    return fileEditor.getUserData(UpdateCourseNotificationProvider.KEY)
  }

  private fun createCourse(isUpToDate: Boolean): EduCourse {
    val course = courseWithFiles {
      lesson {
        eduTask("task1") {
          taskFile("Task.txt")
        }
      }
    }.asRemote(EduNames.STUDY)
    course.isUpToDate = isUpToDate
    return course
  }
}
