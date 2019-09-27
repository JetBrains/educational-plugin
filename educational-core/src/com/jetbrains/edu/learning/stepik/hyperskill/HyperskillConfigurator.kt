package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillTopic
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import javax.swing.JPanel

interface HyperskillConfigurator<T> : EduConfigurator<T> {

  override fun getTestFileName() = ""

  override fun additionalTaskTab(currentTask: Task?, project: Project): Pair<JPanel, String>? {
    if (currentTask == null) return null

    val course = currentTask.lesson.course
    if (course is HyperskillCourse && course.isStudy) {
      if (!course.isTaskInProject(currentTask)) return null
      val topicsPanel = AdditionalTabPanel(project)
      topicsPanel.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)

      val topics = course.taskToTopics[currentTask.index - 1]
      var descriptionText = "<h3 ${StyleManager().textStyleHeader}>Topics for current stage :</h3>"
      if (topics != null) {
        for (topic in topics) {
          descriptionText += topicLink(topic)
          descriptionText += "<br>"
        }
      }
      else {
        descriptionText += "<a ${StyleManager().textStyleHeader}>No topics found for current stage."
      }
      topicsPanel.setText(descriptionText)

      return Pair(topicsPanel, "Topics")
    }
    return null
  }

  private fun topicLink(topic: HyperskillTopic): String =
    "<a ${StyleManager().textStyleHeader};color:${linkColor()} href=\"https://hyperskill.org/learn/step/${topic.theoryId}/\">${topic.title}</a>"

  private fun linkColor(): String = if (UIUtil.isUnderDarcula()) "#6894C6" else "#5C84C9"
}
