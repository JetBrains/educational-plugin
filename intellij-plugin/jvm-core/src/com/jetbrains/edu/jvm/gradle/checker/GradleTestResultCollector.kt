package com.jetbrains.edu.jvm.gradle.checker

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.jetbrains.edu.learning.checker.tests.SMTestResultCollector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.xmlEscaped

class GradleTestResultCollector : SMTestResultCollector() {

  override fun getErrorMessage(node: SMTestProxy): String {
    // @formatter:off
    val message = node.errorMessage ?:
                  // Gradle integration doesn't pass error messages in some cases in 2023.3
                  // See https://youtrack.jetbrains.com/issue/EDU-6567
                  node.stacktrace?.extractMessageFromStackTrace() ?:
                  EduCoreBundle.message("error.execution.failed")
    // @formatter:on
    return message.xmlEscaped
  }

  override fun getComparisonErrorMessage(node: SMTestProxy): String = extractComparisonErrorMessage(node).xmlEscaped

  companion object {
    // Each stacktrace line after error message starts with some space symbols and `at`
    private val STACK_TRACE_LINE_START: Regex = """^\s*at""".toRegex()

    private fun String.extractMessageFromStackTrace(): String? {
      val builder = StringBuilder()

      // The first line of stacktrace starts with a fully qualified name of exception class with `: ` after it
      val firstLine = lineSequence().firstOrNull()?.substringAfter(": ") ?: return null
      builder.append(firstLine)

      for (line in lineSequence().drop(1)) {
        if (STACK_TRACE_LINE_START.find(line) != null) {
          break
        }
        builder.appendLine()
        builder.append(line)
      }
      return builder.toString()
    }
  }
}
