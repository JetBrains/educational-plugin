package com.jetbrains.edu.javascript.learning.checker

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.jetbrains.edu.learning.checker.tests.SMTestResultCollector

class JsTestResultCollector : SMTestResultCollector() {

  // It is tested only with Jest so may not work with other JS test frameworks
  override fun getComparisonErrorMessage(node: SMTestProxy): String = extractComparisonErrorMessage(node)

  override fun getErrorMessage(node: SMTestProxy): String {
    // we suppress the hardcoded string literal inspection here because the failedMessageStart is not visible for users,
    // but is highlighted by the inspection when passed to the function `substringAfter`
    @Suppress("HardCodedStringLiteral")
    val failedMessageStart = "Failed: \""
    val errorMessage = node.errorMessage.orEmpty()
    return if (errorMessage.startsWith(failedMessageStart)) {
      errorMessage.substringAfter(failedMessageStart).substringBeforeLast('"').replace("\\\"", "\"")
    }
    else {
      errorMessage
    }
  }
}
