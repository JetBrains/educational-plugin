package com.jetbrains.edu.learning.checkio

import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector

interface CheckiOConnectorProvider {
  val oAuthConnector: CheckiOOAuthConnector
}
