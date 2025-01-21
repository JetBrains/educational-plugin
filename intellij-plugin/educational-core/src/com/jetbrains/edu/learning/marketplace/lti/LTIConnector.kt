package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.service

interface LTIConnector {

  /**
   * returns error message or null, if request was successful
   */
  fun postTaskChecked(onlineService: LTIOnlineService, launchId: String, courseEduId: Int, taskEduId: Int, solved: Boolean): PostTaskSolvedStatus

  companion object {
    fun getInstance(): LTIConnector = service()
  }
}

sealed class PostTaskSolvedStatus
data object Success : PostTaskSolvedStatus()
data object NoLineItem: PostTaskSolvedStatus()
data object ServerError : PostTaskSolvedStatus()
data object UnknownLaunchId : PostTaskSolvedStatus()
data class ConnectionError(val error: String) : PostTaskSolvedStatus()