package com.jetbrains.edu.rust.checker

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.jetbrains.edu.learning.checker.tests.SMTestResultCollector

class RsTestResultCollector : SMTestResultCollector() {

  override fun getErrorMessage(node: SMTestProxy): String {
    val message = super.getErrorMessage(node)
    // We assume that Rust plugin should put correct error message into test node
    // or put all test error output into stacktrace
    if (message.isNotEmpty()) return message
    val stacktrace = node.stacktrace.orEmpty()
    val matchResult = ASSERT_MESSAGE_RE.find(stacktrace) ?: return stacktrace
    return matchResult.groups["message"]?.value ?: error("Failed to find `message` capturing group")
  }

  companion object {
    private val ASSERT_MESSAGE_RE =
      """thread '.*' panicked at '(assertion failed: `\(left (.*) right\)`\s*left: `(.*?)`,\s*right: `(.*?)`(: )?)?(?<message>.*)',"""
        .toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
  }
}
