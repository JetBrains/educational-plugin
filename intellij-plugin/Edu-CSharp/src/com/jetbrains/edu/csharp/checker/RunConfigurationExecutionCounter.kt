package com.jetbrains.edu.csharp.checker

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import java.util.concurrent.atomic.AtomicInteger

@Service(Service.Level.APP)
class RunConfigurationExecutionCounter {
  private val counter = AtomicInteger(0)

  fun getAndIncrement(): Int = counter.getAndIncrement()

  companion object {
    fun getInstance(): RunConfigurationExecutionCounter = service()
  }
}