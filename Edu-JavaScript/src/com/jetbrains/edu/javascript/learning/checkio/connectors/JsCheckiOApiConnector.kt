package com.jetbrains.edu.javascript.learning.checkio.connectors

import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector

object JsCheckiOApiConnector : CheckiOApiConnector(JsCheckiOOAuthConnector) {
  override val baseUrl: String = JsCheckiONames.JS_CHECKIO_URL
  override val languageId: String = "js"
}