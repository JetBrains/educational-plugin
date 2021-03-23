package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.taskDescription.containsYoutubeLink
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest

class JCefToolWindowLinkHandler(project: Project) : ToolWindowLinkHandler(project) {
  override fun process(url: String): Boolean {
    return when {
      url.contains("about:blank") -> false
      url.startsWith(JCEF_URL_PREFIX) -> super.process(url.substringAfter(JCEF_URL_PREFIX))
      url.containsYoutubeLink() -> false
      else -> super.process(url)
    }
  }

  override fun processExternalLink(url: String): Boolean {
    val urlToOpen = when {
      url.startsWith(StandardFileSystems.FILE_PROTOCOL_PREFIX) -> StepikNames.getStepikUrl() + url.substringAfter(
        StandardFileSystems.FILE_PROTOCOL_PREFIX)
      else -> url
    }
    EduBrowser.getInstance().browse(urlToOpen)
    return true
  }

  companion object {
    private const val JCEF_URL_PREFIX = "file:///jbcefbrowser/"
  }
}

class ToolWindowRequestHandler(private val jcefLinkHandler: JCefToolWindowLinkHandler) : CefRequestHandlerAdapter() {
  /**
   * Called before browser navigation. If the navigation is canceled LoadError will be called with an ErrorCode value of Aborted.
   *
   * @return true to cancel the navigation or false to allow the navigation to proceed.
   */
  override fun onBeforeBrowse(browser: CefBrowser?,
                              frame: CefFrame?,
                              request: CefRequest?,
                              user_gesture: Boolean,
                              is_redirect: Boolean): Boolean {
    val url = request?.url ?: return false
    return jcefLinkHandler.process(url)
  }
}

class TaskInfoLifeSpanHandler(private val jcefLinkHandler: JCefToolWindowLinkHandler) : CefLifeSpanHandlerAdapter() {
  override fun onBeforePopup(browser: CefBrowser?, frame: CefFrame?, targetUrl: String?, targetFrameName: String?): Boolean {
    if (targetUrl == null) return true
    return jcefLinkHandler.process(targetUrl)
  }
}