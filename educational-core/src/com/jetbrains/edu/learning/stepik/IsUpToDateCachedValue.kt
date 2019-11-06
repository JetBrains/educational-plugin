package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.courseFormat.EduCourse
import java.util.concurrent.TimeUnit

class IsUpToDateCachedValue(private val course: EduCourse) {
  private val timeoutMs: Long = TimeUnit.HOURS.toMillis(5)
  private var lastCalcTime: Long = 0
  private var cache: Boolean = true

  @Synchronized
  fun get(): Boolean {
    if (hasUpToDateValue()) {
      return cache
    }
    resetCachedValue(course.checkIsUpToDate())
    return cache
  }

  @Synchronized
  private fun hasUpToDateValue(): Boolean {
    return timeoutMs > System.currentTimeMillis() - lastCalcTime
  }

  @Synchronized
  fun resetCachedValue(value: Boolean) {
    lastCalcTime = System.currentTimeMillis()
    cache = value
  }
}