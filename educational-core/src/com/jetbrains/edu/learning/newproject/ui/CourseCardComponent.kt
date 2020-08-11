package com.jetbrains.edu.learning.newproject.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.CourseMetaInfo
import com.jetbrains.edu.learning.CoursesStorage
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.coursePanel.*
import com.jetbrains.edu.learning.projectView.ProgressUtil
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import icons.EducationalCoreIcons
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JComponent
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

private const val INFO_HGAP = 0
private const val INFO_VGAP = 5

private const val ACADEMY_TEXT = "Log in and select a project to start"
private const val NO_TASKS_COMPLETED_YET = "No tasks completed yet"
private const val COMPLETED = "Completed"

private val HOVER_COLOR: Color = JBColor.namedColor("BrowseCourses.hoverBackground", JBColor(0xF5F9FF, 0x282A2C))
private val SELECTION_COLOR: Color = JBColor.namedColor("BrowseCourses.lightSelectionBackground", JBColor(0xE9EEF5, 0x36393B))
val GRAY_COLOR: Color = JBColor.namedColor("BrowseCourses.infoForeground", JBColor(Gray._120, Gray._135))

class CourseCardComponent(val courseInfo: CourseInfo, errorHandler: (ErrorState) -> Unit) : JPanel(BorderLayout()) {
  private val logoComponent: JLabel = JLabel()
  private var courseNameInfoComponent: CourseNameInfoComponent = CourseNameInfoComponent(courseInfo, errorHandler)

  init {
    border = JBUI.Borders.empty(CARD_GAP)
    logoComponent.isOpaque = false
    logoComponent.icon = courseInfo.course.getScaledLogo(LOGO_SIZE, this@CourseCardComponent)
    logoComponent.border = JBUI.Borders.emptyRight(CARD_GAP)

    add(logoComponent, BorderLayout.LINE_START)
    add(courseNameInfoComponent, BorderLayout.CENTER)

    preferredSize = JBUI.size(CARD_WIDTH, CARD_HEIGHT)
    maximumSize = JBUI.size(CARD_WIDTH, CARD_HEIGHT)
    minimumSize = JBUI.size(CARD_WIDTH, CARD_HEIGHT)

    updateColors(false)
  }

  fun updateColors(isSelected: Boolean) {
    updateColors(if (isSelected) SELECTION_COLOR else MAIN_BG_COLOR)
  }

  private fun updateColors(background: Color) {
    UIUtil.setBackgroundRecursively(this, background)
  }

  fun setSelection(isSelectedOrHover: Boolean, scroll: Boolean = false) {
    if (scroll) {
      scrollToVisible()
    }
    updateColors(isSelectedOrHover)
    repaint()
  }

  fun setHover() {
    updateColors(HOVER_COLOR)
  }

  private fun scrollToVisible() {
    val parent = parent as JComponent
    val bounds = bounds
    if (!parent.visibleRect.contains(bounds)) {
      parent.scrollRectToVisible(bounds)
    }
  }

  fun updateButton() {
    courseNameInfoComponent.updateButton(courseInfo)
  }
}

class CourseNameInfoComponent(courseInfo: CourseInfo, errorHandler: (ErrorState) -> Unit) : JPanel(BorderLayout()) {
  private val nameComponent: CourseNameComponent = CourseNameComponent(courseInfo, errorHandler)
  private val courseInfoComponent: JPanel

  init {
    val course = courseInfo.course
    val courseMetaInfo = CoursesStorage.getInstance().getCourseMetaInfo(course)
    courseInfoComponent = when {
      courseMetaInfo != null -> {
        MyCourseInfoComponent(courseMetaInfo)
      }
      course is EduCourse -> {
        CommunityCourseInfoComponent(course)
      }
      else -> {
        AcademyCourseInfoComponent(course)
      }
    }
    add(nameComponent, BorderLayout.NORTH)
    add(courseInfoComponent, BorderLayout.SOUTH)
  }

  fun updateButton(courseInfo: CourseInfo) {
    nameComponent.updateButton(courseInfo)
  }
}

class CourseNameComponent(courseInfo: CourseInfo, errorHandler: (ErrorState) -> Unit) : JPanel(BorderLayout()) {
  private val nameLabel: JLabel = JLabel()
  private val button: CourseButtonBase

  init {
    nameLabel.text = courseInfo.course.name
    nameLabel.font = Font(TypographyManager().bodyFont, Font.BOLD, FONT_SIZE)

    val coursePath = CoursesStorage.getInstance().getCoursePath(courseInfo.course)
    button = when {
      courseInfo.course is JetBrainsAcademyCourse -> {
        JBAcademyCourseButton(errorHandler, false)
      }
      coursePath != null -> {
        OpenCourseButton()
      }
      else -> {
        StartCourseButton(errorHandler, false)
      }
    }.apply {
      addListener(courseInfo)
      isEnabled = canStartCourse(courseInfo)
    }

    add(nameLabel, BorderLayout.CENTER)
    add(button, BorderLayout.EAST)
  }

  fun updateButton(courseInfo: CourseInfo) {
    button.isEnabled = button.canStartCourse(courseInfo)
  }
}

class CommunityCourseInfoComponent(course: EduCourse) : JPanel(FlowLayout(FlowLayout.LEFT, INFO_HGAP, INFO_VGAP)) {
  private val rating: JLabel = JLabel()
  private val downloads: JLabel = JLabel()
  private val authorComponent: JLabel = JLabel()

  init {
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

class AcademyCourseInfoComponent(course: Course?) : JPanel(FlowLayout(FlowLayout.LEFT, INFO_HGAP, INFO_VGAP)) {
  private val commentLabel = JLabel()

  init {
    commentLabel.foreground = GRAY_COLOR
    commentLabel.font = Font(TypographyManager().bodyFont, Font.PLAIN, SMALL_FONT_SIZE)

    if (course is JetBrainsAcademyCourse) {
      commentLabel.text = ACADEMY_TEXT
    }
    add(commentLabel)
  }
}

class MyCourseInfoComponent(courseMetaInfo: CourseMetaInfo) : JPanel(FlowLayout(FlowLayout.LEFT, INFO_HGAP, INFO_VGAP)) {
  private val progressBar: JProgressBar = ProgressUtil.createProgressBar()
  private var progressLabel: JLabel = JLabel()

  init {
    val tasksSolved = courseMetaInfo.tasksSolved
    val tasksTotal = courseMetaInfo.tasksTotal
    progressLabel.foreground = GRAY_COLOR

    when (tasksSolved) {
      0 -> {
        progressLabel.text = NO_TASKS_COMPLETED_YET
      }
      tasksTotal -> {
        progressLabel.text = COMPLETED
      }
      else -> {
        progressLabel.text = "${tasksSolved}/${tasksTotal}"

        progressBar.apply {
          border = JBUI.Borders.emptyRight(H_GAP)
          maximum = tasksTotal
          value = tasksSolved
        }
        add(progressBar)
      }
    }
    add(progressLabel)
  }
}


