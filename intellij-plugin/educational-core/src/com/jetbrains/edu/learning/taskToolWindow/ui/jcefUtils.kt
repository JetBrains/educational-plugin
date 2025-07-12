package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.JBColor
import com.intellij.ui.jcef.JBCefBrowser
import com.jetbrains.edu.learning.RemoteEnvHelper
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingBasedTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel.getIconPath
import com.jetbrains.edu.learning.taskToolWindow.links.JCefToolWindowLinkHandler
import com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries.ChoiceTaskQueryManager
import com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries.SortingBasedTaskQueryManager
import com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries.TableTaskQueryManager
import com.jetbrains.edu.learning.taskToolWindow.ui.jcefSpecificQueries.TaskQueryManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.ChoiceTaskResourcesManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleResourcesManager.INTELLIJ_ICON_QUICKFIX_OFF_BULB
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.TableTaskResourcesManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.TaskToolWindowBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.sortingBasedTask.MatchingTaskResourcesManager
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.sortingBasedTask.SortingTaskResourcesManager
import com.jetbrains.edu.learning.xmlEscaped
import kotlinx.css.*
import kotlinx.css.properties.BoxShadow
import kotlinx.css.properties.BoxShadows
import kotlinx.css.properties.deg
import kotlinx.css.properties.rotate
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import org.jsoup.nodes.Element

class JCEFToolWindowRequestHandler(private val jcefLinkHandler: JCefToolWindowLinkHandler) : CefRequestHandlerAdapter() {
  /**
   * Called before browser navigation. If the navigation is canceled LoadError will be called with an ErrorCode value of Aborted.
   *
   * @return true to cancel the navigation or false to allow the navigation to proceed.
   */
  override fun onBeforeBrowse(
    browser: CefBrowser?,
    frame: CefFrame?,
    request: CefRequest?,
    user_gesture: Boolean,
    is_redirect: Boolean
  ): Boolean {
    val url = request?.url ?: return false

    // Load everything inside iframes. It is usually an embedded content such as YouTube video or anything else.
    if (request.transitionType == CefRequest.TransitionType.TT_AUTO_SUBFRAME) {
      return false
    }

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
private const val HINT_BLOCK_TEMPLATE: String = """
                                          <div class="$HINT_HEADER">
                                            <img src="%s" style="display: inline-block;"> %s %s 
                                          </div>
                                          <div class="hint_content">
                                            %s
                                          </div>
                                          """
private const val HINT_EXPANDED_BLOCK_TEMPLATE: String = """
                                                   <div class="$HINT_HEADER_EXPANDED">
                                                     <img src="%s" style="display: inline-block;"> %s %s 
                                                   </div>
                                                   <div class='hint_content'>
                                                     %s
                                                   </div>
                                                   """
fun wrapHintJCEF(project: Project, hintElement: Element, displayedHintNumber: String, hintTitle: String): String {
  val course = StudyTaskManager.getInstance(project).course
  val hintText: String = hintElement.html()
  val escapedHintTitle = hintTitle.xmlEscaped
  if (course == null) {
    return String.format(HINT_BLOCK_TEMPLATE, escapedHintTitle, displayedHintNumber, hintText)
  }

  val bulbIcon = if (!RemoteEnvHelper.isRemoteDevServer()) {
    StyleResourcesManager.resourceUrl(INTELLIJ_ICON_QUICKFIX_OFF_BULB)
  }
  else {
    "https://intellij-icons.jetbrains.design/icons/AllIcons/expui/codeInsight/quickfixOffBulb.svg"
  }
  val bulbIconWithTheme = if (!isUnitTestMode) getIconPath(bulbIcon) else ""

  val study = course.isStudy
  return if (study) {
    String.format(HINT_BLOCK_TEMPLATE, bulbIconWithTheme, escapedHintTitle, displayedHintNumber, hintText)
  }
  else {
    String.format(HINT_EXPANDED_BLOCK_TEMPLATE, bulbIconWithTheme, escapedHintTitle, displayedHintNumber, hintText)
  }
}

fun getHTMLTemplateText(task: Task?): String? = when (task) {
  is ChoiceTask -> ChoiceTaskResourcesManager().getText(task)
  is SortingTask -> SortingTaskResourcesManager().getText(task)
  is MatchingTask -> MatchingTaskResourcesManager().getText(task)
  is TableTask -> TableTaskResourcesManager().getText(task)
  else -> null
}

fun getTaskSpecificQueryManager(task: Task?, browserBase: JBCefBrowser): TaskQueryManager<out Task>? = when(task) {
  is ChoiceTask -> ChoiceTaskQueryManager(task, browserBase)
  is SortingBasedTask -> SortingBasedTaskQueryManager(task, browserBase)
  is TableTask -> TableTaskQueryManager(task, browserBase)
  else -> null
}

fun getRadioButtonCSS(): String {
  val styleManager = StyleManager()
  return CssBuilder().apply {
    ".checkbox, .radio" {
      marginTop = 2.px
      marginRight = 9.px
      verticalAlign = VerticalAlign("middle")
      position = Position.relative
      backgroundColor = getRadioButtonBackgroundColor()
      borderWidth = 0.7.px
      borderColor = getBorderColor()
      borderStyle = BorderStyle.solid
      outline = Outline.none
    }
    ".checkbox" {
      borderRadius = 3.px
      //sets size of the element
      padding = Padding(8.px)
    }
    ".radio" {
      borderRadius = 50.pct
      width = 16.px
      height = 16.px
    }
    ".radio:checked" {
      padding = Padding(3.2.px)
      color = styleManager.bodyColor
      backgroundColor = getRadioButtonCheckedBackgroundColor()
      borderColor = getRadioButtonCheckedBorderColor()
      borderWidth = (5.9).px
    }
    ".checkbox:checked" {
      backgroundColor = getRadioButtonCheckedBorderColor()
      borderColor = getRadioButtonCheckedBorderColor()
    }
    ".checkbox:checked:after" {
      display = Display.block
    }
    ".checkbox:after" {
      display = Display.none
      position = Position.absolute
      content = QuotedString("")
      left = 6.px
      top = 2.px
      width = 3.px
      height = 8.px
      backgroundColor = getRadioButtonCheckedBorderColor()
      borderStyle = BorderStyle.solid
      borderColor = getRadioButtonCheckedBackgroundColor()
      borderTopWidth = 0.px
      borderBottomWidth = 2.px
      borderLeftWidth = 0.px
      borderRightWidth = 2.px
      transform.rotate(35.deg)
    }
    ".radio:focus, .radio:before, .radio:hover, .checkbox:focus, .checkbox:before, .checkbox:hover" {
      boxShadow += BoxShadow(color = getRadioButtonFocusColor(), 0.px, 0.px, 2.px, 2.px)
    }
    ".disable:focus, .disable:before, .disable:hover" {
      boxShadow = BoxShadows.none
    }
  }.toString()
    .plus(".checkbox, .radio { -webkit-appearance: none; }")
    .plus(getRadioButtonSystemSpecificCss())
}

private fun getBorderColor(): Color {
  return if (!JBColor.isBright()) {
    Color(TaskToolWindowBundle.value("darcula.choice.options.background.color"))
  }
  else {
    Color(TaskToolWindowBundle.value("choice.options.border.color"))
  }
}


private fun getRadioButtonCheckedBorderColor(): Color {
  return if (!JBColor.isBright()) {
    Color(TaskToolWindowBundle.value("darcula.choice.options.background.color"))
  }
  else {
    Color(TaskToolWindowBundle.value("choice.options.checked.border.color"))
  }
}

private fun getRadioButtonCheckedBackgroundColor(): Color {
  return if (!JBColor.isBright()) {
    Color(TaskToolWindowBundle.value("choice.options.border.color"))
  }
  else {
    Color(TaskToolWindowBundle.value("choice.options.background.color"))
  }
}

private fun getRadioButtonFocusColor(): Color {
  return if (!JBColor.isBright()) {
    Color(TaskToolWindowBundle.value("darcula.choice.options.focus.color"))
  }
  else {
    Color(TaskToolWindowBundle.value("choice.options.focus.color"))
  }
}

private fun getRadioButtonBackgroundColor(): Color {
  return if (!JBColor.isBright()) {
    Color(TaskToolWindowBundle.value("darcula.choice.options.background.color"))
  }
  else {
    Color(TaskToolWindowBundle.value("choice.options.background.color"))
  }
}

private fun getRadioButtonSystemSpecificCss(): String {
  if (SystemInfo.isWindows) {
    return CssBuilder().apply {
      ".radio:checked" {
        marginRight = 7.3.px
        left = (-1).px
      }
      ".checkbox:checked" {
        borderStyle = BorderStyle.none
        padding = Padding(8.7.px)
      }
    }.toString()
  }
  return ""
}