package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.taskToolWindow.containsYoutubeLink
import org.apache.commons.lang3.StringUtils

class JCefToolWindowLinkHandler(project: Project) : ToolWindowLinkHandler(project) {

  /**
   * For each type of custom links url looks different:
   *
   * 1) `course://lesson1/task1/file` -> `course://lesson1/task1/file1`
   * 2) `psi_element://java.lang.String#contains` -> `file:///jbcefbrowser/psi_element://java.lang.String#contains`
   * 3) `file://lesson1/task1/file.txt` -> `file://lesson1/task1/file.txt`
   * 4) relative path
   * `./path/index.html` -> `file:///jbcefbrowser/path/index.html` or `/path/index.html` -> `file:///path/index.html`
   * 5) external links `https://www.jetbrains.com` -> `https://www.jetbrains.com/`
   *
   * @return false if need to continue (for example open external link at task description), otherwise true
   */
  override fun process(url: String, referUrl: String?): Boolean {
    // this links we can open in task description and don't open in browser
    if (url.contains("about:blank") ||
        url.containsYoutubeLink() ||
        StringUtils.isNotBlank(referUrl) // for example: open link from youtube in task description
    ) {
      return false
    }

    when {
      containsMoreThanOneProtocol(url) && url.startsWith(JCEF_URL_PREFIX) -> {
        val cleanedUrl = url.substringAfter(JCEF_URL_PREFIX)
        super.process(cleanedUrl, null)
      }
      url.startsWith(JCEF_URL_PREFIX) -> processExternalLink(url.replace(JBCEF_BROWSER, ""))
      else -> super.process(url, null)
    }
    return true
  }

  /**
   * For example:
   * `file:///jbcefbrowser/psi_element://java.lang.String#contains`
   */
  private fun containsMoreThanOneProtocol(url: String): Boolean {
    return url.split(URLUtil.SCHEME_SEPARATOR).size > 2
  }

  companion object {
    private const val JBCEF_BROWSER: String = "/jbcefbrowser/"
    private const val JCEF_URL_PREFIX: String = "${StandardFileSystems.FILE_PROTOCOL_PREFIX}$JBCEF_BROWSER"
  }
}
