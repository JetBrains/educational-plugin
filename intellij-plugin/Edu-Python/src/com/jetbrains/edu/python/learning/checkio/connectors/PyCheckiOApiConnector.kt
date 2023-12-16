package com.jetbrains.edu.python.learning.checkio.connectors

import com.jetbrains.edu.learning.checkio.connectors.CheckiOApiConnector
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames

object PyCheckiOApiConnector : CheckiOApiConnector(PyCheckiOOAuthConnector) {
  override val baseUrl: String = PyCheckiONames.PY_CHECKIO_URL
  override val languageId: String = "py"
}