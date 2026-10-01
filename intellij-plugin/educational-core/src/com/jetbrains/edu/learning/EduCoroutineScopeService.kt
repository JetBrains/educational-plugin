package com.jetbrains.edu.learning

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

/**
 * Provides a project-level coroutine scope for code that doesn't have its own service to inject a scope into.
 * The scope is cancelled when the project is closed.
 */
@Service(Service.Level.PROJECT)
class EduCoroutineScopeService(val scope: CoroutineScope) {
  companion object {
    fun getInstance(project: Project): EduCoroutineScopeService = project.service()
  }
}
