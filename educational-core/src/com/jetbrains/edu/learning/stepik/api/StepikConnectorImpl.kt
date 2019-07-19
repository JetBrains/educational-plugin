package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.stepik.StepikNames

class StepikConnectorImpl : StepikConnector() {
  override val baseUrl: String = StepikNames.STEPIK_URL
}
