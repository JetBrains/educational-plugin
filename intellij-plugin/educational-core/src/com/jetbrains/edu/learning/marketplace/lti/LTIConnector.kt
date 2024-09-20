package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.network.executeCall
import com.jetbrains.edu.learning.onError
import okhttp3.ConnectionPool

@Service(Service.Level.APP)
class LTIConnector {

  private val connectionPool: ConnectionPool = ConnectionPool()

  /**
   * returns error message or null, if request was successful
   */
  fun postTaskSolved(launchId: String, courseEduId: Int, taskEduId: Int): PostTaskSolvedStatus {
    val retrofit = createRetrofitBuilder(SubmissionsServiceHost.getSelectedUrl(), connectionPool, LTIAuthBundle.value("ltiServiceToken")).build()
    val ltiEndpoints = retrofit.create(LTIEndpoints::class.java)

    val response = ltiEndpoints.reportTaskSolved(launchId, courseEduId, taskEduId).executeCall(omitErrors = true).onError {
      return ConnectionError(it)
    }

    return when (response.code()) {
      200 -> NoLineItem
      204 -> Success
      404 -> UnknownLaunchId
      else -> return ServerError // 500 or other statuses
    }
  }

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