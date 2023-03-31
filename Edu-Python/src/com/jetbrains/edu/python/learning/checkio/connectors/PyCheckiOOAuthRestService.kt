package com.jetbrains.edu.python.learning.checkio.connectors

import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthRestService
import com.jetbrains.edu.learning.checkio.utils.CheckiONames

class PyCheckiOOAuthRestService private constructor() : CheckiOOAuthRestService(CheckiONames.PY_CHECKIO, PyCheckiOOAuthConnector)
