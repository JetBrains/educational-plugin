package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.icons.AllIcons
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.joinCourse
import com.jetbrains.edu.learning.newproject.ui.getScaledLogo
import com.jetbrains.edu.learning.projectView.ProgressUtil
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import icons.EducationalCoreIcons
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar

private const val CARD_GAP = 10
private const val CARD_WIDTH = 80
private const val CARD_HEIGHT = 70
private const val H_GAP = 10
private const val LOGO_SIZE = 40
private const val FONT_SIZE = 13
private const val SMALL_FONT_SIZE = 12

private const val ACADEMY_TEXT = "Log in and select a project to start"

@Suppress("unused") // TODO: Use when "my courses" implemented
private const val NO_TASKS_COMPLETED_YET = "No tasks completed yet"

@Suppress("unused") // TODO: Use when "my courses" implemented
private const val COMPLETED = "Completed"

private val HOVER_COLOR: Color = JBColor.namedColor("BrowseCourses.lightSelectionBackground", JBColor(0xE9EEF5, 0x36393B))
private val GRAY_COLOR: Color = JBColor.namedColor("BrowseCourses.infoForeground", JBColor(Gray._120, Gray._135))

class CourseCardComponent(course: Course?) : JPanel(BorderLayout()) {
  private val logoComponent: JLabel = JLabel()
  private var courseNameInfoComponent: CourseNameInfoComponent = CourseNameInfoComponent(course)

  init {
    border = JBUI.Borders.empty(CARD_GAP)
    logoComponent.isOpaque = false
    logoComponent.icon = course?.getScaledLogo(LOGO_SIZE, this@CourseCardComponent)
    logoComponent.border = JBUI.Borders.empty(0, 0, 0, CARD_GAP)

    add(logoComponent, BorderLayout.LINE_START)
    add(courseNameInfoComponent, BorderLayout.CENTER)

    preferredSize = JBUI.size(CARD_WIDTH, CARD_HEIGHT)
    maximumSize = JBUI.size(CARD_WIDTH, CARD_HEIGHT)

    toolTipText = course?.name
    updateColors(false)
  }

  fun updateColors(isSelectedOrHover: Boolean) {
    updateColors(if (!isSelectedOrHover) TaskDescriptionView.getTaskDescriptionBackgroundColor() else HOVER_COLOR)
  }

  private fun updateColors(background: Color) {
    UIUtil.setBackgroundRecursively(this, background)
  }
}

class CourseNameInfoComponent(course: Course?) : JPanel(BorderLayout()) {
  private val nameComponent: CourseNameComponent = CourseNameComponent(course)
  private val courseInfoComponent: JPanel = if (course is EduCourse) CommunityCourseInfoComponent(course)
  else AcademyCourseInfoComponent(course)

  init {
    add(nameComponent, BorderLayout.NORTH)
    add(courseInfoComponent, BorderLayout.SOUTH)
  }
}

class CourseNameComponent(course: Course?) : JPanel(BorderLayout()) {
  private val nameLabel: JLabel = JLabel()

  private val openStartLoginButton: StartCourseButtonBase = OpenCourseButton { courseInfo, mode ->
    joinCourse(courseInfo, mode, {}, {})
  }

  init {
    nameLabel.text = course?.name
    nameLabel.font = Font(TypographyManager().bodyFont, Font.BOLD, FONT_SIZE)

    add(nameLabel, BorderLayout.CENTER)
    add(openStartLoginButton, BorderLayout.EAST)
  }
}

class CommunityCourseInfoComponent(course: EduCourse) : JPanel() {
  private val rating: JLabel = JLabel()
  private val downloads: JLabel = JLabel()
  private val authorComponent: JLabel = JLabel()

  init {
    (layout as FlowLayout).alignment = FlowLayout.LEFT
    (layout as FlowLayout).hgap = 0

    rating.foreground = GRAY_COLOR
    rating.border = JBUI.Borders.emptyRight(H_GAP)
    if (course.reviewScore != 0.0) {
      rating.icon = AllIcons.Plugins.Rating
      rating.text = "%.${1}f".format(course.reviewScore)
    }
    else {
      rating.text = EduCoreBundle.message("course.dialog.card.not.rated")
    }
    rating.font = Font(TypographyManager().bodyFont, Font.PLAIN, SMALL_FONT_SIZE)

    downloads.foreground = GRAY_COLOR
    downloads.icon = EducationalCoreIcons.User
    downloads.text = course.learnersCount.toString()
    downloads.border = JBUI.Borders.emptyRight(H_GAP)
    downloads.font = Font(TypographyManager().bodyFont, Font.PLAIN, SMALL_FONT_SIZE)

    val authors = course.authorFullNames.joinToString()
    authorComponent.isVisible = authors.isNotEmpty()
    authorComponent.foreground = GRAY_COLOR
    authorComponent.font = Font(TypographyManager().bodyFont, Font.PLAIN, SMALL_FONT_SIZE)
    if (authorComponent.isVisible) {
      authorComponent.text = authors
    }

    add(rating)
    add(downloads)
    add(authorComponent)
  }
}

class AcademyCourseInfoComponent(course: Course?) : JPanel() {
  private val commentLabel = JLabel()

  init {
    (layout as FlowLayout).alignment = FlowLayout.LEFT
    (layout as FlowLayout).hgap = 0

    commentLabel.foreground = GRAY_COLOR
    commentLabel.font = Font(TypographyManager().bodyFont, Font.PLAIN, SMALL_FONT_SIZE)

    if (course is JetBrainsAcademyCourse) {
      commentLabel.text = ACADEMY_TEXT
    }
    add(commentLabel)
  }
}

@Suppress("unused") // TODO: use when "my courses" implemented
class MyCourseInfoComponent(course: Course) : JPanel(FlowLayout()) {
  private var progressBar: JProgressBar = JProgressBar()
  private var progressLabel: JLabel = JLabel()

  init {
    (layout as FlowLayout).alignment = FlowLayout.LEFT
    (layout as FlowLayout).hgap = 0

    val (taskSolved, tasksTotal) = ProgressUtil.countProgress(course)

    progressBar = ProgressUtil.createProgressBar()
    progressBar.border = JBUI.Borders.empty(0, 0, 0, H_GAP)
    progressBar.maximum = tasksTotal
    progressBar.value = taskSolved

    progressLabel.foreground = GRAY_COLOR
    progressLabel.text = "${taskSolved}/${tasksTotal}"

    add(progressBar)
    add(progressLabel)
  }
}


