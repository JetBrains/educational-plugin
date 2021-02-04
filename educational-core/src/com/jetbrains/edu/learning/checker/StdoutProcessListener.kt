package com.jetbrains.edu.learning.checker

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.Key

class StdoutProcessListener : ProcessAdapter() {
  private val _output: MutableList<String> = mutableListOf()

  val output: List<String> get() = _output

  override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
    if (outputType == ProcessOutputTypes.STDOUT) {
      _output.add(event.text)
    }
  }
}
