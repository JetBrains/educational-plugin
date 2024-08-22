package com.jetbrains.edu.coursecreator.testGeneration

import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator

class ProgressIndicator : CustomProgressIndicator {
  override fun cancel() {
    // TODO
  }

  override fun getFraction(): Double {
    return 0.92
  }

  override fun getText(): String {
    return "TODO" // TODO
  }

  override fun isCanceled(): Boolean {
    return false
  }

  override fun isIndeterminate(): Boolean {
    return false
  }

  override fun setFraction(value: Double) {

  }

  override fun setIndeterminate(value: Boolean) {

  }

  override fun setText(text: String) {

  }

  override fun start() {

  }

  override fun stop() {

  }
}