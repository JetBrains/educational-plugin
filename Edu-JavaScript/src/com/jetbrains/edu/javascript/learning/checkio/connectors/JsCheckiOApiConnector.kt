package com.jetbrains.edu.javascript.learning.checkio.connectors

import com.jetbrains.edu.javascript.learning.checkio.utils.JsCheckiONames
import com.jetbrains.edu.learning.checkio.api.RetrofitUtils
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector

object JsCheckiOApiConnector :
  CheckiOApiConnector(RetrofitUtils.createRetrofitApiInterface(JsCheckiONames.JS_CHECKIO_API_HOST), JsCheckiOOAuthConnector) {

  override fun getLanguageId(): String = "js"
}