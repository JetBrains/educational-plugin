package com.jetbrains.edu.coursecreator.feedback

import com.intellij.ui.HyperlinkLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.ui.CCNewCoursePanel
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.feedback.CourseFeedbackInfoData
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jdesktop.swingx.HorizontalLayout
import javax.swing.JLabel
import javax.swing.JPanel

fun createFeedbackPanel(courseTitleField: CCNewCoursePanel.CourseTitleField, course: Course): JPanel {
  val panel = JPanel(HorizontalLayout())
  val message = JLabel(EduCoreBundle.message("ui.feedback.cc.label"))
  message.apply {
    border = JBUI.Borders.emptyRight(3)
    foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
  }

  val hyperlinkLabel = HyperlinkLabel(EduCoreBundle.message("ui.feedback.cc.hyperlink.label"))

  hyperlinkLabel.addHyperlinkListener {
    val dialog = CCInIdeFeedbackDialog(CourseFeedbackInfoData.from(course, courseTitleField.text))
    dialog.showAndGet()
  }
  panel.add(message)
  panel.add(hyperlinkLabel)
  return panel
}