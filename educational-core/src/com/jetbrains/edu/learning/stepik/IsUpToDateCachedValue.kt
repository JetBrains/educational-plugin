package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.progress.ProgressManager
import com.jetbrains.edu.learning.courseFormat.EduCourse
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class IsUpToDateCachedValue(private val course: EduCourse) {
  private val timeoutMs: Long = TimeUnit.HOURS.toMillis(5)
  private var lastCalcTime: Long = 0
  private var cache: Boolean = true
  private val lock = ReentrantLock()

  fun get(): Boolean {
    //BACKCOMPAT: since 2019.3 use ProgressIndicatorUtils.computeWithLockAndCheckingCanceled
    try {
      while (!lock.tryLock(50, TimeUnit.MILLISECONDS)) {
        ProgressManager.checkCanceled()
      }
      if (hasUpToDateValue()) {
        return cache
      }
      resetCachedValue(course.checkIsUpToDate())
      return cache
    } finally {
      lock.unlock()
    }
  }

  private fun hasUpToDateValue(): Boolean {
    return timeoutMs > System.currentTimeMillis() - lastCalcTime
  }

  fun resetCachedValue(value: Boolean) {
    lock.withLock {
      lastCalcTime = System.currentTimeMillis()
      cache = value
    }
  }
}