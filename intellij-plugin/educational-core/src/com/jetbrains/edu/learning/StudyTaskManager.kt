package com.jetbrains.edu.learning

import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import com.jetbrains.edu.learning.agreement.UserAgreementSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.isHyperskillProject
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.loadCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.isEduYamlProject
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.startSynchronization
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Implementation of class which contains all the information about study in context of current project
 */
@Service(Service.Level.PROJECT)
class StudyTaskManager(private val project: Project) : DumbAware, Disposable, EduTestAware {
  @Volatile
  private var courseLoadedWithError = false

  private val courseLoadingLock = ReentrantLock()

  @Volatile
  private var _course: Course? = null

  var course: Course?
    get() = _course
    set(course) {
      _course = course
      course?.fireCourseSetEvent()
    }

  private fun Course.fireCourseSetEvent() =
    project.messageBus.syncPublisher(COURSE_SET).courseSet(this)

  private fun needToLoadCourse(project: Project): Boolean =
    !project.isDefault
    && !LightEdit.owns(project)
    && course == null
    && !courseLoadedWithError
    && project.isEduYamlProject()
    && UserAgreementSettings.getInstance().isPluginAllowed
    && !project.isHyperskillProject

  private fun initializeCourse() {
    if (!needToLoadCourse(project)) return

    // Read action is necessary for the `loadCourse` call.
    // At the same time, ordering of lock acquiring is important here:
    // read/write action lock should be taken first, `courseLoadingLock` after that.
    //
    // Otherwise, it can lead to deadlock if:
    // - 1st thread: takes write action and waits for `courseLoadingLock` taken by 2nd thread
    // - 2nd thread: takes `courseLoadingLock` and waits for write action from 1st thread to finish to take read lock
    // Described case may happen when the plugin is dynamically loaded with an already opened project
    val (loadedCourse, courseLoadingHappened) = runReadAction {
      courseLoadingLock.withLock {
        if (!needToLoadCourse(project)) return@runReadAction null to false

        val loadedCourse = try {
          loadCourse(project)
        }
        catch (th: Throwable) {
          LOG.error("Error while loading course", th)
          null
        }

        courseLoadedWithError = loadedCourse == null
        if (loadedCourse != null) {
          logger<StudyTaskManager>().info("Loaded course corresponding to the project: ${loadedCourse.name}")
          _course = loadedCourse
        }
        else {
          logger<StudyTaskManager>().info("Course corresponding to the project loaded with errors")
        }
        loadedCourse to true
      }
    }

    if (courseLoadingHappened) {
      loadedCourse?.fireCourseSetEvent()
      startSynchronization(project)
    }
  }

  override fun dispose() {}

  @TestOnly
  override fun cleanUpState() {
    course = null
  }

  companion object {
    val COURSE_SET = Topic.create("Edu.courseSet", CourseSetListener::class.java)
    val LOG = logger<StudyTaskManager>()

    fun getInstance(project: Project): StudyTaskManager = project.service<StudyTaskManager>().apply {
      initializeCourse()
    }
  }
}
