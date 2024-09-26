package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator

class TestProgressIndicator(private val indicator: ProgressIndicator) : CustomProgressIndicator {
  override fun setText(text: String) {
    indicator.text = text
  }

  override fun getText(): String = indicator.text

  override fun setIndeterminate(value: Boolean) {
    indicator.isIndeterminate = value
  }

  override fun isIndeterminate(): Boolean = indicator.isIndeterminate

  override fun setFraction(value: Double) {
    indicator.fraction = value
  }

  override fun getFraction(): Double = indicator.fraction

  override fun isCanceled(): Boolean = indicator.isCanceled

  override fun start() {
    indicator.start()
  }

  override fun stop() {
    indicator.stop()
  }

  override fun cancel() {
    indicator.cancel()
  }
}