package com.jetbrains.edu.learning.checker

import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.CheckStatus

class CheckResult @JvmOverloads constructor(
  val status: CheckStatus,
  val message: String,
  val details: String? = null,
  private val isEscaped: Boolean = false
) {

  val escapedMessage: String get() = message.escaped
  val escapedDetails: String? get() = details?.escaped

  private val String.escaped: String get() = if (isEscaped) this else StringUtil.escapeXml(this)

  companion object {
    @JvmField val NO_LOCAL_CHECK = CheckResult(CheckStatus.Unchecked, "Local check isn't available")
    @JvmField val FAILED_TO_CHECK = CheckResult(CheckStatus.Unchecked, CheckUtils.FAILED_TO_CHECK_MESSAGE)
    @JvmField val LOGIN_NEEDED = CheckResult(CheckStatus.Unchecked, CheckUtils.LOGIN_NEEDED_MESSAGE)
    @JvmField val CONNECTION_FAILED = CheckResult(CheckStatus.Unchecked, "Connection failed")
  }
}
