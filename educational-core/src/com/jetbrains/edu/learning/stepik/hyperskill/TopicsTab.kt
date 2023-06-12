package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.HtmlUIMode
import com.jetbrains.edu.learning.taskDescription.ui.htmlTransformers.TaskDescriptionTransformer
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabType.TOPICS_TAB
import com.jetbrains.edu.learning.ui.EduColors

class TopicsTab(project: Project) : AdditionalTab(project, TOPICS_TAB) {

  override val uiMode: HtmlUIMode
    get() = HtmlUIMode.SWING

  init {
    init()
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

    val transformationContext = HtmlTransformerContext(project, task, HtmlUIMode.SWING)
    setText(TaskDescriptionTransformer.transform(descriptionText, transformationContext))
  }

  private fun topicLink(topic: HyperskillTopic, textStyleHeader: String): String {
    val liStyle = "style=color:#${ColorUtil.toHex(EduColors.hyperlinkColor)};"
    val linkStyle = "$textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)}"
    val topicLink = "https://hyperskill.org/learn/step/${topic.theoryId}/"

    return """<li $liStyle><a $linkStyle href=$topicLink>${topic.title}</a></li>"""
  }
}
