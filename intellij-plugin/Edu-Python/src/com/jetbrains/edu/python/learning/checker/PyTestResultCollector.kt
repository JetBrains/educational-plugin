package com.jetbrains.edu.python.learning.checker

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.jetbrains.edu.learning.checker.tests.SMTestResultCollector

class PyTestResultCollector : SMTestResultCollector() {

  override fun getErrorMessage(node: SMTestProxy): String {
    return node.stacktrace?.lineSequence()?.firstOrNull { it.startsWith(ASSERTION_ERROR) }?.substringAfter(ASSERTION_ERROR)
           ?: node.errorMessage.orEmpty()
  }

  companion object {
    private const val ASSERTION_ERROR = "AssertionError: "
  }
}
