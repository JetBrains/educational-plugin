package com.jetbrains.edu.learning.checker

class CollectingLoggedErrorProcessor : CollectingLoggedErrorProcessorBase() {

  override fun processError(category: String, message: String?, t: Throwable?, details: Array<out String>): Boolean {
    if (t is AssertionError) {
      _exceptions += t
    }
    return super.processError(category, message, t, details)
  }
}
