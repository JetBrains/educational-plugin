package com.jetbrains.edu.ai.validation.core.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.validation.core.model.UserResult

@Service(Service.Level.PROJECT)
class UserResultsService {
  private val userResults = mutableListOf<UserResult>()

  fun addResult(result: UserResult) {
    userResults.add(result)
  }

  fun getResults(): List<UserResult> = userResults.toList()

  companion object {
    fun getInstance(project: Project): UserResultsService {
      return project.getService(UserResultsService::class.java)
    }
  }
}
