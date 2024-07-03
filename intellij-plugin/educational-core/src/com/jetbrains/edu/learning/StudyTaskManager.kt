package com.jetbrains.edu.learning

import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.loadCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.startSynchronization
import org.jetbrains.annotations.TestOnly

/**
 * Implementation of class which contains all the information about study in context of current project
 */
@Service(Service.Level.PROJECT)
class StudyTaskManager(private val project: Project) : DumbAware, Disposable, LightTestAware {
  @Volatile
  private var courseLoadedWithError = false

  private var _course: Course? = null

  var course: Course?
    get() = _course
    set(course) {
      _course = course
      course?.apply {
        project.messageBus.syncPublisher(COURSE_SET).courseSet(this)
      }
    }

  override fun dispose() {}

  @TestOnly
  override fun cleanUpState() {
    course = null
  }

  companion object {
    val COURSE_SET = Topic.create("Edu.courseSet", CourseSetListener::class.java)

    fun getInstance(project: Project): StudyTaskManager {
      val manager = project.service<StudyTaskManager>()
      if (!project.isDefault && !LightEdit.owns(project) && manager.course == null
          && project.isEduYamlProject() && !manager.courseLoadedWithError) {
        val course = loadCourse(project)
        manager.courseLoadedWithError = course == null
        if (course != null) {
          manager.course = course
        }
        startSynchronization(project)
      }
      return manager
    }
  }
}
