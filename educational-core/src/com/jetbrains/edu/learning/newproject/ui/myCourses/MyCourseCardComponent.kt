package com.jetbrains.edu.learning.newproject.ui.myCourses

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseMetaInfo
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.GRAY_COLOR
import com.jetbrains.edu.learning.projectView.ProgressUtil
import java.awt.Component
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

private const val H_GAP = 10

private const val INFO_HGAP = 0
private const val INFO_VGAP = 5

class MyCourseCardComponent(course: Course) : CourseCardComponent(course) {

  override fun createSideActionComponent(): JComponent {
    return createRemoveCourseComponent()
  }

  override fun getClickComponent(): Component {
    return baseComponent
  }

  override fun onHover(isSelected: Boolean) {
    super.onHover(isSelected)
    setActionComponentVisible(true)
  }

  override fun onHoverEnded() {
    setActionComponentVisible(false)
  }

  override fun createBottomComponent(): JPanel {
    val courseMetaInfo = CoursesStorage.getInstance().getCourseMetaInfo(course) ?: error("Cannot find ${course.name} in storage")
    return MyCourseInfoComponent(courseMetaInfo)
  }

  private fun createRemoveCourseComponent(): JComponent {
    val removeLabel = JBLabel(AllIcons.Diff.Remove)

    removeLabel.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        val courseMetaInfo = CoursesStorage.getInstance().getCourseMetaInfo(course) ?: error("Cannot find ${course.name} in storage")
        val location = courseMetaInfo.location

        // We want to set default option to make dialog work correctly on windows
        val result = Messages.showDialog(this@MyCourseCardComponent,
                                         EduCoreBundle.message("course.dialog.my.courses.remove.course.text", courseMetaInfo.name),
                                         EduCoreBundle.message("course.dialog.my.courses.remove.course.title"),
                                         arrayOf(Messages.getCancelButton(),
                                                 EduCoreBundle.message("course.dialog.my.courses.remove.course")),
                                         Messages.OK,
                                         Messages.getErrorIcon())

        if (result == Messages.NO) {
          CoursesStorage.getInstance().removeCourseByLocation(location)
        }
      }
    })

    return Wrapper(removeLabel).apply {
      isEnabled = false
      isVisible = false
    }
  }
}

private class MyCourseInfoComponent(courseMetaInfo: CourseMetaInfo) : JPanel(FlowLayout(FlowLayout.LEFT, INFO_HGAP, INFO_VGAP)) {

  init {
    val tasksSolved = courseMetaInfo.tasksSolved
    val tasksTotal = courseMetaInfo.tasksTotal

    if (courseMetaInfo.courseMode != CCUtils.COURSE_MODE && (tasksSolved != 0 && tasksSolved != tasksTotal)) {
      val progressBar = ProgressUtil.createProgressBar().apply {
        border = JBUI.Borders.emptyRight(H_GAP)
        maximum = tasksTotal
        value = tasksSolved
      }
      add(progressBar)
    }

    val isStudentMode = courseMetaInfo.courseMode == CourseMode.STUDY
    val infoLabel = JLabel().apply {
      foreground = GRAY_COLOR
      text = when (tasksSolved) {
        0 -> if (tasksTotal != 0 && isStudentMode) EduCoreBundle.message("course.dialog.my.courses.card.no.tasks") else ""
        tasksTotal -> EduCoreBundle.message("course.dialog.my.courses.card.completed")
        else -> "${tasksSolved}/${tasksTotal}"
      }
    }
    add(infoLabel)
  }
}
