package com.jetbrains.edu.ai.error.explanation

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class ErrorExplanationStderrStorage(private val project: Project) {
  private var stderr: String? = null

  fun getStderr(): String? = stderr

  fun setStderr(stderr: String) {
    this.stderr = stderr
  }

  companion object {
    fun getInstance(project: Project): ErrorExplanationStderrStorage = project.service()
  }
}