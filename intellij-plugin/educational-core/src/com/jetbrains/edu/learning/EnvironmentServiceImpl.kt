package com.jetbrains.edu.learning

class EnvironmentServiceImpl : EnvironmentService {
  override val isUnitTestMode: Boolean
    get() = com.jetbrains.edu.learning.isUnitTestMode
}
