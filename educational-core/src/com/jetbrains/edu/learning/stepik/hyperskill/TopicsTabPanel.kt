package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.EduBrowserHyperlinkListener
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.ui.EduColors

class TopicsTabPanel(project: Project,
                     course: HyperskillCourse,
                     task: Task
) : AdditionalTabPanel(project, EduCoreBundle.message("hyperskill.topics.tab.name")) {

  init {
    addHyperlinkListener(EduBrowserHyperlinkListener.INSTANCE)

    val topics = course.taskToTopics[task.index - 1]
    var descriptionText = "<h3 ${StyleManager().textStyleHeader}>${EduCoreBundle.message("hyperskill.topics.for.stage")}</h3>"
    if (topics != null) {
      for (topic in topics) {
        descriptionText += topicLink(topic)
        descriptionText += "<br>"
      }
    }
    else {
      descriptionText += "<a ${StyleManager().textStyleHeader}>${EduCoreBundle.message("hyperskill.topics.not.found")}"
    }
    setText(descriptionText)
  }

  private fun topicLink(topic: HyperskillTopic): String =
    "<a ${StyleManager().textStyleHeader};color:${ColorUtil.toHex(
      EduColors.hyperlinkColor)} href=\"https://hyperskill.org/learn/step/${topic.theoryId}/\">${topic.title}</a>"
}
