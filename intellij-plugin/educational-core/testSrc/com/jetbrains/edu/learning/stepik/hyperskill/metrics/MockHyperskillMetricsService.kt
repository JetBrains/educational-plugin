package com.jetbrains.edu.learning.stepik.hyperskill.metrics

class MockHyperskillMetricsService : HyperskillMetricsService() {

  /**
   * In tests operations happen quickly, so we need to imitate time spent on a task otherwise it is always too close to zero
   */
  override fun toDuration(start: Long): Double {
    return DEFAULT_TIME_SPENT
  }

  companion object {
    private const val DEFAULT_TIME_SPENT = 20.12345
  }
}