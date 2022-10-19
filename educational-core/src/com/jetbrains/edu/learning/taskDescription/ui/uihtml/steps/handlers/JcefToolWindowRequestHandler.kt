package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.handlers

import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest

class JcefToolWindowRequestHandler(private val jcefLinkHandler: JcefToolWindowLinkHandler) : CefRequestHandlerAdapter() {
  /**
   * Called before browser navigation. If the navigation is canceled LoadError will be called with an ErrorCode value of Aborted.
   *
   * @return true to cancel the navigation or false to allow the navigation to proceed.
   */
  override fun onBeforeBrowse(
    browser: CefBrowser?,
    frame: CefFrame?,
    request: CefRequest?,
    userGesture: Boolean,
    isRedirect: Boolean
  ): Boolean {
    val url = request?.url ?: return false
    val referUrl = request.referrerURL
    return jcefLinkHandler.process(url, referUrl)
  }
}