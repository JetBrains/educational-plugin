package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.addCourseCardInfoStyle
import com.jetbrains.edu.learning.newproject.ui.createUsersNumberLabel
import com.jetbrains.edu.learning.newproject.ui.humanReadableDuration
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JComponent
import javax.swing.JPanel

class CodeforcesCardComponent(course: Course) : CourseCardComponent(course) {

  override fun isLogoVisible(): Boolean {
    return false
  }

  override fun createBottomComponent(): JComponent {
    val panel = JPanel(HorizontalLayout(0)).apply {
      border = JBUI.Borders.emptyBottom(10)
    }
    val startDate = (course as CodeforcesCourse).startDate
    panel.add(createDateLabel(startDate))

    if (course.isRegistrationOpen || course.isPastContest) {
      panel.add(createUsersNumberLabel(course.participantsNumber))
    }
    else {
      panel.add(createRegistrationOpensInLabel(course.registrationCountdown), HorizontalLayout.RIGHT)
    }

    return panel
  }

  private fun createDateLabel(startDate: ZonedDateTime?): JBLabel {
    if (startDate == null) {
      return JBLabel()
    }

    val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
    return JBLabel().apply {
      text = startDate.format(dateTimeFormatter)
      addCourseCardInfoStyle()
    }
  }

  private fun createRegistrationOpensInLabel(registrationCountdown: Duration?): JBLabel {
    if (registrationCountdown == null) {
      return JBLabel()
    }
    return JBLabel().apply {
      val humanReadableDuration = humanReadableDuration(registrationCountdown, false)
      text = EduCoreBundle.message("codeforces.course.selection.registration.opens.in", humanReadableDuration)
      addCourseCardInfoStyle()
    }
  }
}