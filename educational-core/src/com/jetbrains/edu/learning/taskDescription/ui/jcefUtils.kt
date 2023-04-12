package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.io.URLUtil
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.taskDescription.containsYoutubeLink
import com.jetbrains.edu.learning.taskDescription.ui.jcefSpecificQueries.ChoiceTaskQueryManager
import com.jetbrains.edu.learning.taskDescription.ui.jcefSpecificQueries.MatchingTaskQueryManager
import com.jetbrains.edu.learning.taskDescription.ui.jcefSpecificQueries.SortingTaskQueryManager
import com.jetbrains.edu.learning.taskDescription.ui.jcefSpecificQueries.TaskQueryManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.ChoiceTaskResourcesManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.sortingBasedTask.MatchingTaskResourcesManager
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.sortingBasedTask.SortingTaskResourcesManager
import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.lang3.StringUtils
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.*
import org.cef.network.CefRequest
import org.jsoup.nodes.Element

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
      containsMoreThanOneProtocol(url) -> super.process(url, null)
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

  override fun processExternalLink(url: String) {
    if (url.startsWith(StandardFileSystems.FILE_PROTOCOL_PREFIX)) {
      processExternalRelativePath(url)
    }
    else {
      super.processExternalLink(url)
    }
  }

  /**
   * html can contain relative paths.
   * for example: <a>/path/index.html</a>, <a>./path/index.html</a>
   */
  private fun processExternalRelativePath(url: String) {
    if (project.course is StepikCourse) {
      val stepikUrl = StepikNames.getStepikUrl() + url.substringAfter(StandardFileSystems.FILE_PROTOCOL_PREFIX)
      super.processExternalLink(stepikUrl)
    }
    else {
      LOG.warn("Can't open relative path on stepik for course ${project.course?.name}")
    }
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
    val referUrl = request.referrerURL
    return jcefLinkHandler.process(url, referUrl)
  }
}

class JCEFTaskInfoLifeSpanHandler(private val jcefLinkHandler: JCefToolWindowLinkHandler) : CefLifeSpanHandlerAdapter() {
  override fun onBeforePopup(browser: CefBrowser?, frame: CefFrame?, targetUrl: String?, targetFrameName: String?): Boolean {
    if (targetUrl == null) return true
    return jcefLinkHandler.process(targetUrl)
  }
}

private const val HINT_HEADER: String = "hint_header"
private const val HINT_HEADER_EXPANDED: String = "$HINT_HEADER checked"
private const val HINT_BLOCK_TEMPLATE: String = "<div class='" + HINT_HEADER + "'>%s %s</div>" +
                                                "  <div class='hint_content'>" +
                                                " %s" +
                                                "  </div>"
private const val HINT_EXPANDED_BLOCK_TEMPLATE: String = "<div class='" + HINT_HEADER_EXPANDED + "'>%s %s</div>" +
                                                         "  <div class='hint_content'>" +
                                                         " %s" +
                                                         "  </div>"

fun wrapHintJCEF(project: Project, hintElement: Element, displayedHintNumber: String, hintTitle: String): String {
  val course = StudyTaskManager.getInstance(project).course
  val hintText: String = hintElement.html()
  val escapedHintTitle = StringEscapeUtils.escapeHtml(hintTitle)
  if (course == null) {
    return String.format(HINT_BLOCK_TEMPLATE, escapedHintTitle, displayedHintNumber, hintText)
  }

  val study = course.isStudy
  return if (study) {
    String.format(HINT_BLOCK_TEMPLATE, escapedHintTitle, displayedHintNumber, hintText)
  }
  else {
    String.format(HINT_EXPANDED_BLOCK_TEMPLATE, escapedHintTitle, displayedHintNumber, hintText)
  }
}

fun getHTMLTemplateText(task: Task?): String? = when (task) {
  is ChoiceTask -> ChoiceTaskResourcesManager().getText(task)
  is SortingTask -> SortingTaskResourcesManager().getText(task)
  is MatchingTask -> MatchingTaskResourcesManager().getText(task)
  else -> null
}

fun getTaskSpecificQueryManager(task: Task?, browserBase: JBCefBrowser): TaskQueryManager<out Task>? = when(task) {
  is ChoiceTask -> ChoiceTaskQueryManager(task, browserBase)
  is SortingTask -> SortingTaskQueryManager(task, browserBase)
  is MatchingTask -> MatchingTaskQueryManager(task, browserBase)
  else -> null
}