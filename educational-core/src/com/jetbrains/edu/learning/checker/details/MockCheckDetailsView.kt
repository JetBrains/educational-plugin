package com.jetbrains.edu.learning.checker.details

import javax.swing.JComponent

class MockCheckDetailsView : CheckDetailsView() {

  private var message: String? = null

  override fun showOutput(message: String) {
    this.message = message
  }

  override fun showCheckResultDetails(title: String, message: String) {
    this.message = message
  }

  override fun showResult(title: String, panel: JComponent) {
  }

  override fun clear() {
    message = null
  }

  fun getMessage(): String? = message
}
