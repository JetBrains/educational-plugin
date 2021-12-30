package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StepikCourse
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
      url.startsWith(JCEF_URL_PREFIX) -> processExternalLink(url.replace(JBCEF_BROWSER, ""))
      url.containsYoutubeLink() -> false
      else -> super.process(url)
    }
  }

  override fun processExternalLink(url: String): Boolean {
    if (url.startsWith(StandardFileSystems.FILE_PROTOCOL_PREFIX)) {
      return if (project.course is StepikCourse) {
        // html can contain relative paths.
        // for example: <a>/path/index.html</a>, <a>./path/index.html</a>
        val stepikUrl = StepikNames.getStepikUrl() + url.substringAfter(StandardFileSystems.FILE_PROTOCOL_PREFIX)
        super.processExternalLink(stepikUrl)
      }
      else {
        LOG.warn("Can't open relative path on stepik for course ${project.course?.name}")
        false
      }
    }
    return super.processExternalLink(url)
  }

  companion object {
    private const val JBCEF_BROWSER: String = "/jbcefbrowser/"
    private const val JCEF_URL_PREFIX: String = "${StandardFileSystems.FILE_PROTOCOL_PREFIX}$JBCEF_BROWSER"
    private val LOG = Logger.getInstance(JCefToolWindowLinkHandler::class.java)
  }
}

class JCEFToolWindowRequestHandler(private val jcefLinkHandler: JCefToolWindowLinkHandler) : CefRequestHandlerAdapter() {
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

class JCEFTaskInfoLifeSpanHandler(private val jcefLinkHandler: JCefToolWindowLinkHandler) : CefLifeSpanHandlerAdapter() {
  override fun onBeforePopup(browser: CefBrowser?, frame: CefFrame?, targetUrl: String?, targetFrameName: String?): Boolean {
    if (targetUrl == null) return true
    return jcefLinkHandler.process(targetUrl)
  }
}