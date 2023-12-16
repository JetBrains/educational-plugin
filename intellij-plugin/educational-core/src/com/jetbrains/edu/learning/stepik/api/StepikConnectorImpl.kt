package com.jetbrains.edu.learning.stepik.api

import com.jetbrains.edu.learning.stepik.StepikNames

class StepikConnectorImpl : StepikConnector() {
  // Do not convert it into property with initializer
  // because [stepikUrl] can be changed by user
  override val baseUrl: String
    get() = StepikNames.getStepikUrl()
}
