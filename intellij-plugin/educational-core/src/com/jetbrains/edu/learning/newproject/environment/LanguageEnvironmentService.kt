package com.jetbrains.edu.learning.newproject.environment

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolder
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.cancelOnDispose
import com.jetbrains.edu.learning.Err
import com.jetbrains.edu.learning.ModalityStateProvider
import com.jetbrains.edu.learning.Result
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A helper service to work with environment catalogs. Has its own scope to execute long-running operations.
 */
@Service(Service.Level.APP)
class LanguageEnvironmentService(private val scope: CoroutineScope) {

  fun <E: LanguageEnvironment> loadEnvironmentCatalog(
    environmentCatalogProvider: LanguageEnvironmentCatalogProvider<E>,
    course: Course,
    context: UserDataHolder?,
    modalityStateProvider: ModalityStateProvider,
    disposable: Disposable,
    environmentsLoaded: suspend (Result<LanguageEnvironmentCatalog<E>, String>) -> Unit
  ) {
    modalityStateProvider.waitForModality(disposable) { modalityState ->
      scope.launch(Dispatchers.IO + modalityState.asContextElement()) {
        val environmentCatalog = environmentCatalogProvider.collectEnvironmentsForCourse(course, context)
        if (environmentCatalog is Err) {
          LOG.warn("Failed to load environments for ${course.name}: ${environmentCatalog.error}")
        }
        environmentsLoaded(environmentCatalog)
      }.cancelOnDispose(disposable)
    }
  }

  fun installLanguageEnvironment(project: Project, course: Course, environment: LanguageEnvironment, finished: (InstallationResult) -> Unit) {
    scope.launch {
      withBackgroundProgress(project, EduCoreBundle.message("generate.course.set.up.language.environment.progress.text")) {
        val result = withContext(Dispatchers.IO) {
          environment.installIfNeeded(project, course)
        }
        withContext(Dispatchers.EDT) {
          finished(result)
        }
      }
    }
  }

  companion object {
    private val LOG = logger<LanguageEnvironmentService>()
    fun getInstance(): LanguageEnvironmentService = service()
  }
}