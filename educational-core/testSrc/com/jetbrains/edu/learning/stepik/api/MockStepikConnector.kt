package com.jetbrains.edu.learning.stepik.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler

class MockStepikConnector : StepikConnector() {

  private val helper = MockWebServerHelper(ApplicationManager.getApplication())

  private var _baseUrl: String? = null

  public override val baseUrl: String
    get() {
      return _baseUrl ?: helper.baseUrl
    }

  fun setBaseUrl(baseUrl: String, disposable: Disposable) {
    _baseUrl = baseUrl
    Disposer.register(disposable, Disposable {
      _baseUrl = null
    })
  }

  fun setHelperBaseUrl() {
    _baseUrl = helper.baseUrl
  }

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockStepikConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }
}
