package com.jetbrains.edu.learning.stepik.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler

class MockStepikConnector : StepikConnector() {

  private val helper = MockWebServerHelper(ApplicationManager.getApplication())

  private var _baseUrl: String? = null

  public override var baseUrl: String
    get() {
      return _baseUrl ?: helper.baseUrl
    }
    set(value) {
      _baseUrl = value
    }

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockStepikConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }
}
