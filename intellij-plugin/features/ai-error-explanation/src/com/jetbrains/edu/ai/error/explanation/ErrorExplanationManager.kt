package com.jetbrains.edu.ai.error.explanation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.Service.Level
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Service(Level.PROJECT)
class ErrorExplanationManager {
  private val prevStderr = MutableStateFlow<String?>(null)

  @Suppress("unused")
  val stderr: StateFlow<String?> = prevStderr.asStateFlow()

  fun setStderr(stderr: String?) {
    prevStderr.value = stderr
  }

  companion object {
    fun getInstance(project: Project): ErrorExplanationManager = project.service()
  }
}