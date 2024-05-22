package com.jetbrains.edu.learning.taskToolWindow.ui.jcef

import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefClient
import com.intellij.ui.jcef.JCEFHtmlPanel

class TaskSpecificJBCefBrowser : JCEFHtmlPanel(true, JBCefApp.getInstance().createClient(), null) {

  init {
    jbCefClient.setProperty(JBCefClient.Properties.JS_QUERY_POOL_SIZE, TASK_SPECIFIC_PANEL_JS_QUERY_POOL_SIZE)
  }

  companion object {
    // maximum number of created qs queries in taskSpecificQueryManager
    private const val TASK_SPECIFIC_PANEL_JS_QUERY_POOL_SIZE = 2
  }
}