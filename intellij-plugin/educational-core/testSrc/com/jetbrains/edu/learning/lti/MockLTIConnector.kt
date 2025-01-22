package com.jetbrains.edu.learning.lti

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler
import com.jetbrains.edu.learning.marketplace.lti.LTIConnectorImpl
import com.jetbrains.edu.learning.marketplace.lti.LTIOnlineService

class MockLTIConnector : LTIConnectorImpl(), Disposable {

  private val helper = MockWebServerHelper(this)

  override fun getUrlForService(onlineService: LTIOnlineService): String = helper.baseUrl

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockLTIConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }

  override fun dispose() {}
}