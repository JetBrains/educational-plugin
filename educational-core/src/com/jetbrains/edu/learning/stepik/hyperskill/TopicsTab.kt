package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.TOPICS_TAB
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.ListenersAdder
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.CodeHighlighter
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.MediaThemesAndExternalLinkIconsTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps.ResourceWrapper
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.viewers.SwingUIHtmlViewer
import com.jetbrains.edu.learning.ui.EduColors
import javax.swing.JComponent

class TopicsTab(project: Project) : AdditionalTab(project, TOPICS_TAB) {
  // for some reason, that used to be only a swing viewer
  private val htmlViewer = SwingUIHtmlViewer(
    project,
    MediaThemesAndExternalLinkIconsTransformer then CodeHighlighter then ResourceWrapper then ListenersAdder
  )
  override val innerTextPanel: JComponent
    get() = htmlViewer.component

  init {
    setupTextViewer()
    Disposer.register(this, htmlViewer)
  }

  override fun update(task: Task) {
    val course = task.course as? HyperskillCourse
                 ?: error("Topics tab is designed for Hyperskill course, but task is located in different course")
    val topics = course.taskToTopics[task.index - 1]
    val descriptionText = buildString {
      val textStyleHeader = StyleManager().textStyleHeader
      appendLine("<h3 $textStyleHeader;padding:0;>${EduCoreBundle.message("hyperskill.topics.for.stage")}</h3>")
      if (topics != null) {
        appendLine("<ol $textStyleHeader;padding-top:4px>")
        for (topic in topics) {
          appendLine(topicLink(topic, textStyleHeader))
        }
        appendLine("</ol>")
      }
      else {
        appendLine("<a $textStyleHeader>${EduCoreBundle.message("hyperskill.topics.not.found")}")
      }
    }

    htmlViewer.setHtmlWithContext(descriptionText, HtmlTransformerContext(project, task))
  }

  private fun topicLink(topic: HyperskillTopic, textStyleHeader: String): String {
    val liStyle = "style=color:#${ColorUtil.toHex(EduColors.hyperlinkColor)};"
    val linkStyle = "$textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)}"
    val topicLink = "https://hyperskill.org/learn/step/${topic.theoryId}/"

    return """<li $liStyle><a $linkStyle href=$topicLink>${topic.title}</a></li>"""
  }
}
