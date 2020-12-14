package com.jetbrains.edu.learning.newproject.ui.myCourses

import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseMetaInfo
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.GRAY_COLOR
import com.jetbrains.edu.learning.projectView.ProgressUtil
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar

private const val H_GAP = 10

private const val INFO_HGAP = 0
private const val INFO_VGAP = 5
private const val NO_TASKS_COMPLETED_YET = "No tasks completed yet"
private const val COMPLETED = "Completed"

class MyCourseCardComponent(course: Course) : CourseCardComponent(course) {

  override fun createCourseInfoComponent(): JPanel {
    val courseMetaInfo = course as CourseMetaInfo
    return MyCourseInfoComponent(courseMetaInfo)
  }
}

private class MyCourseInfoComponent(courseMetaInfo: CourseMetaInfo) : JPanel(FlowLayout(FlowLayout.LEFT, INFO_HGAP, INFO_VGAP)) {
  private val progressBar: JProgressBar = ProgressUtil.createProgressBar()
  private var infoLabel: JLabel = JLabel()

  init {
    infoLabel.foreground = GRAY_COLOR

    if (courseMetaInfo.courseMode != CCUtils.COURSE_MODE) {
      createProgressBar(courseMetaInfo)
    }
    add(infoLabel)
  }

  private fun createProgressBar(courseMetaInfo: CourseMetaInfo) {
    val tasksSolved = courseMetaInfo.tasksSolved
    val tasksTotal = courseMetaInfo.tasksTotal

    when (tasksSolved) {
      0 -> {
        infoLabel.text = NO_TASKS_COMPLETED_YET
      }
      tasksTotal -> {
        infoLabel.text = COMPLETED
      }
      else -> {
        infoLabel.text = "${tasksSolved}/${tasksTotal}"

        progressBar.apply {
          border = JBUI.Borders.emptyRight(H_GAP)
          maximum = tasksTotal
          value = tasksSolved
        }
        add(progressBar)
      }
    }
  }
}
