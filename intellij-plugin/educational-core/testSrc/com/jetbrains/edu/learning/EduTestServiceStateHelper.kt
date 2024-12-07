package com.jetbrains.edu.learning

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.marketplace.update.MarketplaceUpdateChecker
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.stepik.hyperskill.metrics.HyperskillMetricsService
import com.jetbrains.edu.learning.stepik.hyperskill.update.HyperskillCourseUpdateChecker
import com.jetbrains.edu.learning.storage.LearningObjectsStorageManager
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.yaml.YamlLoadingErrorManager
import kotlin.reflect.KClass

/**
 * Helps manage state of services marked with [EduTestAware] between tests cases
 */
object EduTestServiceStateHelper {

  private val testAwareApplicationServices: List<KClass<out EduTestAware>> = listOf(
    CoursesStorage::class,
    EduBrowser::class,
    HyperskillMetricsService::class,
  )

  private val testAwareProjectServices: List<KClass<out EduTestAware>> = listOf(
    TaskToolWindowView::class,
    CCFrameworkLessonManager::class,
    FrameworkLessonManager::class,
    SubmissionsManager::class,
    YamlLoadingErrorManager::class,
    MarketplaceUpdateChecker::class,
    HyperskillCourseUpdateChecker::class,
    LearningObjectsStorageManager::class,
    // Intentionally the last item in the list since it's holds the essential knowledge about project course
    StudyTaskManager::class,
  )

  fun restoreState(project: Project?) {
    performForAllServices(project, EduTestAware::restoreState)
  }

  fun cleanUpState(project: Project?) {
    // Some services may be used in scheduled tasks (via `invokeLater`, for example).
    // And since we can't rely on service/project disposing in light tests, let's wait for all these tasks manually
    runInEdtAndWait {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
    }

    performForAllServices(project, EduTestAware::cleanUpState)
  }

  private fun performForAllServices(project: Project?, action: EduTestAware.() -> Unit) {
    val application = ApplicationManager.getApplication()
    for (serviceClass in testAwareApplicationServices) {
      // Do not create service if it wasn't created earlier
      application.getServiceIfCreated(serviceClass.java)?.action()
    }
    // There is no necessity to do anything for non-light projects
    // since in heavy tests a new project is created for each test case
    if (project != null && project.isLight) {
      for (serviceClass in testAwareProjectServices) {
        // Do not create service if it wasn't created earlier
        project.getServiceIfCreated(serviceClass.java)?.action()
      }
    }
  }
}
