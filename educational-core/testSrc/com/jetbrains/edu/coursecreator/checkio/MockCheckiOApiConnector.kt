package com.jetbrains.edu.coursecreator.checkio

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.jetbrains.edu.learning.MockWebServerHelper
import com.jetbrains.edu.learning.ResponseHandler
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector

object MockCheckiOApiConnector : CheckiOApiConnector(MockCheckiOOAuthConnector) {
  private val helper = MockWebServerHelper(ApplicationManager.getApplication())

  override val baseUrl: String
    get() = helper.baseUrl

  override val languageId: String = "py"

  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockCheckiOApiConnector {
    helper.addResponseHandler(disposable, handler)
    return this
  }
}