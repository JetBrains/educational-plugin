package com.jetbrains.edu.coursecreator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListener
import com.jetbrains.edu.coursecreator.yaml.YamlDeepLoader
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.saveAll
import com.jetbrains.edu.learning.CourseSetListener
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
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
    // it is also false for newly created courses as config files isn't created yet.
    // it's ok as we don't need to load course from config
    if (myProject.isEduYamlProject()) {
      StudyTaskManager.getInstance(myProject).course = YamlDeepLoader.loadCourse(myProject)
      YamlFormatSynchronizer.startSynchronization(myProject)
    }

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

      YamlFormatSynchronizer.startSynchronization(myProject)
      createYamlConfigFilesIfMissing()
    }
  }

  private fun createYamlConfigFilesIfMissing() {
    val courseDir = myProject.courseDir
    val courseConfig = courseDir.findChild(COURSE_CONFIG)
    if (courseConfig == null) {
      saveAll(myProject)
      FileDocumentManager.getInstance().saveAllDocuments()
    }
  }

  private fun registerListener() {
    if (myTaskFileLifeListener == null) {
      myTaskFileLifeListener = CCVirtualFileListener(myProject)
      VirtualFileManager.getInstance().addVirtualFileListener(myTaskFileLifeListener!!)
    }
  }

  override fun projectClosed() {
    if (myTaskFileLifeListener != null) {
      VirtualFileManager.getInstance().removeVirtualFileListener(myTaskFileLifeListener!!)
    }
  }
}
