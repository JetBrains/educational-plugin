package com.jetbrains.edu.lti

import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.network.executeCall
import com.jetbrains.edu.learning.onError
import okhttp3.ConnectionPool

open class LTIConnectorImpl : LTIConnector {

  private val connectionPool: ConnectionPool = ConnectionPool()

  // to be overridden in tests
  open fun getUrlForService(onlineService: LTIOnlineService): String = onlineService.serviceURL

  override fun postTaskChecked(onlineService: LTIOnlineService, launchId: String, courseEduId: Int, taskEduId: Int, solved: Boolean): PostTaskSolvedStatus {
    val serviceUrl = getUrlForService(onlineService)
    val retrofit = createRetrofitBuilder(serviceUrl, connectionPool, LTIAuthBundle.value("ltiServiceToken")).build()
    val ltiEndpoints = retrofit.create(LTIEndpoints::class.java)

    val response = ltiEndpoints.reportTaskSolved(launchId, courseEduId, taskEduId, solved).executeCall(omitErrors = true).onError {
      return ConnectionError(it)
    }

    return when (response.code()) {
      200 -> NoLineItem
      204 -> Success
      404 -> UnknownLaunchId
      else -> ServerError // 500 or other statuses
    }
  }
}