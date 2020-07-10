package com.jetbrains.edu.learning.checker.details

import javax.swing.JComponent

class MockCheckDetailsView : CheckDetailsView() {
  override fun showOutput(message: String) {
  }

  override fun showCheckResultDetails(title: String, message: String) {
  }

  override fun showResult(title: String, panel: JComponent) {
  }

  override fun clear() {
  }
}