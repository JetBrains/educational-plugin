package com.jetbrains.edu.learning.marketplace.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler

class MockMarketplaceConnector : MarketplaceConnector() {

  private val helper = MockWebServerHelper(ApplicationManager.getApplication())

  override val authUrl: String = helper.baseUrl

  override val repositoryUrl: String = helper.baseUrl

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockMarketplaceConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }
}