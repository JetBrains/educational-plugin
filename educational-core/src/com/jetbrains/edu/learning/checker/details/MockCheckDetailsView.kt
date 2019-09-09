package com.jetbrains.edu.learning.checker.details

import javafx.embed.swing.JFXPanel

class MockCheckDetailsView : CheckDetailsView() {
  override fun showOutput(message: String) {
  }

  override fun showCheckResultDetails(title: String, message: String) {
  }

  override fun showJavaFXResult(title: String, panel: JFXPanel) {
  }

  override fun clear() {
  }
}