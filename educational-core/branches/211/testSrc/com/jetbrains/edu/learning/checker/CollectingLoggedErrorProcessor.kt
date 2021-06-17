package com.jetbrains.edu.learning.checker

import org.apache.log4j.Logger

class CollectingLoggedErrorProcessor : CollectingLoggedErrorProcessorBase() {

  override fun processError(message: String?, t: Throwable?, details: Array<out String>?, logger: Logger) {
    if (t is AssertionError) {
      _exceptions += t
    }
    super.processError(message, t, details, logger)
  }
}
