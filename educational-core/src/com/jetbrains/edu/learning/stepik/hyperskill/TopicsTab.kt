package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.tab.AdditionalTab
import com.jetbrains.edu.learning.taskDescription.ui.tab.SwingTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabManager.TabType.TOPICS_TAB
import com.jetbrains.edu.learning.taskDescription.ui.tab.TabPanel
import com.jetbrains.edu.learning.ui.EduColors

class TopicsTab(project: Project,
                course: HyperskillCourse,
                task: Task
) : AdditionalTab(project, TOPICS_TAB) {

  init {
    addHyperlinkListener(EduBrowserHyperlinkListener.INSTANCE)

    val topics = course.taskToTopics[task.index - 1]
    val descriptionText = buildString {
      val textStyleHeader = StyleManager().textStyleHeader
      appendln("<h3 $textStyleHeader;padding:0;>${EduCoreBundle.message("hyperskill.topics.for.stage")}</h3>")
      if (topics != null) {
        appendln("<ol $textStyleHeader;padding-top:4px>")
        for (topic in topics) {
          appendln(topicLink(topic, textStyleHeader))
        }
        appendln("</ol>")
      }
      else {
        appendln("<a $textStyleHeader>${EduCoreBundle.message("hyperskill.topics.not.found")}")
      }
    }
    setText(descriptionText, plain = false)
  }

  override fun createTabPanel(): TabPanel {
    return SwingTabPanel(project, TOPICS_TAB)
  }

  private fun topicLink(topic: HyperskillTopic, textStyleHeader: String): String {
    val liStyle = "style=color:#${ColorUtil.toHex(EduColors.hyperlinkColor)};"
    val linkStyle = "$textStyleHeader;color:#${ColorUtil.toHex(EduColors.hyperlinkColor)}"
    val topicLink = "https://hyperskill.org/learn/step/${topic.theoryId}/"

    return """<li $liStyle><a $linkStyle href=$topicLink>${topic.title}</a></li>"""
  }
}
