package com.jetbrains.edu.javascript.learning.checkio.connectors

import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthRestService
import com.jetbrains.edu.learning.checkio.utils.CheckiONames

class JsCheckiOOAuthRestService : CheckiOOAuthRestService(CheckiONames.JS_CHECKIO, JsCheckiOOAuthConnector)
