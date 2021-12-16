package com.jetbrains.edu.python.learning.checkio.connectors

import com.jetbrains.edu.learning.checkio.api.RetrofitUtils
import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames

object PyCheckiOApiConnector :
  CheckiOApiConnector(RetrofitUtils.createRetrofitApiInterface(PyCheckiONames.PY_CHECKIO_API_HOST), PyCheckiOOAuthConnector) {

  override val languageId: String = "py"
}