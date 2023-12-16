package com.jetbrains.edu.learning.codeforces.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler

class MockCodeforcesConnector : CodeforcesConnector() {
  private var _baseUrl: String? = null
    set(value) {
      field = value
      serviceHolder.drop()
    }

  private val helper = MockWebServerHelper(ApplicationManager.getApplication())

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

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockCodeforcesConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }
}