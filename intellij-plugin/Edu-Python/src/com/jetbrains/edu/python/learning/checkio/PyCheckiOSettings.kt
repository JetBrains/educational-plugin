package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.jetbrains.edu.learning.checkio.CheckiOSettingsBase
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector

private const val SERVICE_NAME = "PyCheckiOSettings"

@State(name = SERVICE_NAME, storages = [Storage("other.xml")])
class PyCheckiOSettings : CheckiOSettingsBase() {

  override val serviceName: String
    get() = SERVICE_NAME
  override val checkiOOAuthConnector: CheckiOOAuthConnector
    get() = PyCheckiOOAuthConnector

  companion object {
    fun getInstance(): PyCheckiOSettings = service()
  }
}
