package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler

class MockHyperskillConnector : HyperskillConnector() {

  private val helper = MockWebServerHelper(ApplicationManager.getApplication())

  override val baseUrl: String get() = helper.baseUrl

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockHyperskillConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }
}
