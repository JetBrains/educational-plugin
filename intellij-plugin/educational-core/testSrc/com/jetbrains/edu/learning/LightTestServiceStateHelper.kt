package com.jetbrains.edu.learning

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.learning.codeforces.update.CodeforcesCourseUpdateChecker
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.marketplace.update.MarketplaceUpdateChecker
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.stepik.hyperskill.update.HyperskillCourseUpdateChecker
import com.jetbrains.edu.learning.storage.LearningObjectsStorageManager
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.yaml.YamlLoadingErrorManager
import kotlin.reflect.KClass

/**
 * Helps manage state of services marked with [LightTestAware] between light tests cases
 */
object LightTestServiceStateHelper {

  private val lightTestAwareApplicationServices: List<KClass<out LightTestAware>> = listOf(
    CoursesStorage::class,
    EduBrowser::class,
  )

  private val lightTestAwareProjectServices: List<KClass<out LightTestAware>> = listOf(
    TaskToolWindowView::class,
    CCFrameworkLessonManager::class,
    FrameworkLessonManager::class,
    SubmissionsManager::class,
    YamlLoadingErrorManager::class,
    MarketplaceUpdateChecker::class,
    CodeforcesCourseUpdateChecker::class,
    HyperskillCourseUpdateChecker::class,
    LearningObjectsStorageManager::class
  )

  fun restoreState(project: Project) {
    val application = ApplicationManager.getApplication()
    for (serviceClass in lightTestAwareApplicationServices) {
      // Do not create service if it wasn't created earlier
      application.getServiceIfCreated(serviceClass.java)?.restoreState()
    }
    for (serviceClass in lightTestAwareProjectServices) {
      // Do not create service if it wasn't created earlier
      project.getServiceIfCreated(serviceClass.java)?.restoreState()
    }
  }

  fun cleanUpState(project: Project) {
    val application = ApplicationManager.getApplication()
    for (serviceClass in lightTestAwareApplicationServices) {
      // Do not create service if it wasn't created earlier
      application.getServiceIfCreated(serviceClass.java)?.cleanUpState()
    }
    for (serviceClass in lightTestAwareProjectServices) {
      // Do not create service if it wasn't created earlier
      project.getServiceIfCreated(serviceClass.java)?.cleanUpState()
    }
  }
}
