package com.jetbrains.edu.javascript.learning.checkio

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.jetbrains.edu.javascript.learning.checkio.connectors.JsCheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.CheckiOSettingsBase
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector

private const val SERVICE_NAME = "JsCheckiOSettings"

@State(name = SERVICE_NAME, storages = [Storage("other.xml")])
class JsCheckiOSettings : CheckiOSettingsBase() {

  override val serviceName: String
    get() = SERVICE_NAME
  override val checkiOOAuthConnector: CheckiOOAuthConnector
    get() = JsCheckiOOAuthConnector

  companion object {
    fun getInstance(): JsCheckiOSettings = service()
  }
}
