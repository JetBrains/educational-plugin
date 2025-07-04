package com.jetbrains.edu.rust.checker

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.text.nullize
import com.jetbrains.edu.learning.checker.tests.SMTestResultCollector
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.EduTestInfo

// BACKCOMPAT: 2025.2. Drop this class since the corresponding bug is fixed on the platform side
class RsTestResultCollector : SMTestResultCollector() {

  // Workaround for Rust plugin versions where https://youtrack.jetbrains.com/issue/RUST-18152 is not fixed yet
  override fun createEduTestInfo(node: SMTestProxy, name: String): EduTestInfo {
    // We assume that Rust plugin should put a correct non-empty error message into the test node
    // or put all test error output into stacktrace
    if (!node.errorMessage.isNullOrEmpty()) return super.createEduTestInfo(node, name)

    val stacktrace = node.stacktrace.orEmpty()
    val matchResult = ASSERT_MESSAGE_RE.find(stacktrace)
    return if (matchResult == null) {
      node.toEduTestInfo(name, stacktrace, null)
    }
    else {
      val groups = matchResult.groups
      val message = groups["message"]?.value.nullize()
                    ?: groups["fullMessage"]?.value
                    ?: error("Failed to find `message` or `fullMessage` capturing group")

      val diff = if (groups["sign"]?.value == "==") {
        val left = groups["left"]?.value?.unescape()
        val right = groups["right"]?.value?.unescape()
        if (left != null && right != null) CheckResultDiff(right, left) else null
      }
      else {
        null
      }

      node.toEduTestInfo(name, message, diff)
    }
  }

  private fun String.unescape(): String = StringUtil.unquoteString(StringUtil.unescapeStringCharacters(this))

  companion object {
    // Taken from Rust plugin itself with fixed https://youtrack.jetbrains.com/issue/RUST-18152
    private val ASSERT_MESSAGE_RE =
      """thread '.*' panicked at ([^\n]*)\n(?<fullMessage>(assertion `left (?<sign>.*) right` failed(: )?)?(?<message>.*?)\n\s*(left: (?<left>.*?)\n\s*right: (?<right>.*?)\n)?)(note|stack backtrace):"""
        .toRegex(setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL))
  }
}
