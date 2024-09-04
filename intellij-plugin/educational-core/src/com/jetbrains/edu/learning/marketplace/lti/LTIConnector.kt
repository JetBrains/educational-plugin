package com.jetbrains.edu.learning.marketplace.lti

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.jetbrains.edu.learning.marketplace.changeHost.SubmissionsServiceHost
import com.jetbrains.edu.learning.network.createRetrofitBuilder
import com.jetbrains.edu.learning.network.executeParsingErrors
import com.jetbrains.edu.learning.onError
import okhttp3.ConnectionPool

@Service(Service.Level.APP)
class LTIConnector {

  private val connectionPool: ConnectionPool = ConnectionPool()

  /**
   * returns error message or null, if request was successful
   */
  fun postTaskSolved(launchId: String, courseEduId: Int, taskEduId: Int): String? {
    val retrofit = createRetrofitBuilder(SubmissionsServiceHost.getSelectedUrl(), connectionPool, LTIAuthBundle.value("ltiServiceToken")).build()
    val ltiEndpoints = retrofit.create(LTIEndpoints::class.java)

    ltiEndpoints.reportTaskSolved(launchId, courseEduId, taskEduId).executeParsingErrors().onError {
      logger<LTIConnector>().warn("[LTI] Failed to report task solved for task $taskEduId: $it")
      return it
    }

    return null
  }

  companion object {
    fun getInstance(): LTIConnector = service()
  }
}