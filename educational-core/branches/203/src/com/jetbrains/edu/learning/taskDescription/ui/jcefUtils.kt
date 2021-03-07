package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ui.jcef.JBCefBrowser

fun JBCefBrowser.disableErrorPage() {
  jbCefClient.cefClient.removeLoadHandler()
}