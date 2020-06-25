package com.jetbrains.edu.coursecreator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.learning.CourseSetListener
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector

class CCStartupActivity : StartupActivity.DumbAware {

  override fun runActivity(project: Project) {
    val course = StudyTaskManager.getInstance(project).course
    if (course != null) {
      initCCProject(project, course)
    }
    else {
      val connection = project.messageBus.connect()
      connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
        override fun courseSet(course: Course) {
          connection.disconnect()
          initCCProject(project, course)
        }
      })
    }
  }

  private fun initCCProject(project: Project, course: Course) {
    if (!CCUtils.isCourseCreator(project) || isUnitTestMode) return

    val taskManager = StudyTaskManager.getInstance(project)

    ApplicationManager.getApplication().messageBus
      .connect(taskManager)
      .subscribe(VirtualFileManager.VFS_CHANGES, CCVirtualFileListener(project))

    EditorFactory.getInstance().eventMulticaster.addDocumentListener(SynchronizeTaskDescription(project), taskManager)
    EduCounterUsageCollector.eduProjectOpened(course)
  }
}
