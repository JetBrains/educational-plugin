package com.jetbrains.edu.coursecreator

import com.intellij.ide.RecentProjectsManager
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.CourseSetListener
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector

@Suppress("ComponentNotRegistered") // educational-core.xml
class CCProjectComponent(private val myProject: Project) : ProjectComponent {
  private var myTaskFileLifeListener: CCVirtualFileListener? = null

  private fun startTaskDescriptionFilesSynchronization() {
    StudyTaskManager.getInstance(myProject).course ?: return
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(SynchronizeTaskDescription(myProject), myProject)
  }

  override fun getComponentName(): String {
    return "CCProjectComponent"
  }

  override fun projectOpened() {

    if (StudyTaskManager.getInstance(myProject).course != null) {
      initCCProject()
    }
    else {
      val connection = myProject.messageBus.connect()
      connection.subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
        override fun courseSet(course: Course) {
          connection.disconnect()
          initCCProject()
        }
      })
    }
  }

  private fun initCCProject() {
    if (CCUtils.isCourseCreator(myProject)) {
      if (!ApplicationManager.getApplication().isUnitTestMode) {
        registerListener()
      }

      EduCounterUsageCollector.eduProjectOpened(CCUtils.COURSE_MODE)
      startTaskDescriptionFilesSynchronization()
    }
  }

  private fun registerListener() {
    if (myTaskFileLifeListener == null) {
      myTaskFileLifeListener = CCVirtualFileListener(myProject)
      VirtualFileManager.getInstance().addVirtualFileListener(myTaskFileLifeListener!!)
    }
  }

  override fun projectClosed() {
    if (PropertiesComponent.getInstance(myProject).getBoolean(CCCreateCoursePreviewDialog.PREVIEW_FOLDER_PREFIX)) {
      RecentProjectsManager.getInstance().removePath(myProject.basePath)
      RecentProjectsManager.getInstance().updateLastProjectPath()
    }
    if (myTaskFileLifeListener != null) {
      VirtualFileManager.getInstance().removeVirtualFileListener(myTaskFileLifeListener!!)
    }
  }
}
