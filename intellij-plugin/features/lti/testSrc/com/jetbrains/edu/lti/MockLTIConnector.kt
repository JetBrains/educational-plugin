package com.jetbrains.edu.lti

import com.intellij.openapi.Disposable
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler

class MockLTIConnector : LTIConnectorImpl(), Disposable {

  private val helper = MockWebServerHelper(this)

  override fun getUrlForService(onlineService: LTIOnlineService): String = helper.baseUrl

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockLTIConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }

  override fun dispose() {}
}