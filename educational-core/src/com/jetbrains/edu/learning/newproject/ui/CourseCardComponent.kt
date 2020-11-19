package com.jetbrains.edu.learning.newproject.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseMetaInfo
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.newproject.ui.coursePanel.OpenCourseButton
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

private const val INFO_HGAP = 0
private const val INFO_VGAP = 5

private const val ACADEMY_TEXT = "Log in and select a project to start"
private const val NO_TASKS_COMPLETED_YET = "No tasks completed yet"
private const val COMPLETED = "Completed"

private val HOVER_COLOR: Color = JBColor.namedColor("BrowseCourses.hoverBackground", JBColor(0xF5F9FF, 0x282A2C))
private val SELECTION_COLOR: Color = JBColor.namedColor("BrowseCourses.lightSelectionBackground", JBColor(0xE9EEF5, 0x36393B))
val GRAY_COLOR: Color = JBColor.namedColor("BrowseCourses.infoForeground", JBColor(Gray._120, Gray._135))

class CourseCardComponent(val course: Course, showOpenButton: Boolean) : JPanel(BorderLayout()) {
  private val logoComponent: JLabel = JLabel()
  private var courseNameInfoComponent: CourseNameInfoComponent = CourseNameInfoComponent(course, showOpenButton)

  init {
    border = JBUI.Borders.empty(CARD_GAP)
    logoComponent.isOpaque = false
    logoComponent.icon = course.getScaledLogo(JBUI.scale(LOGO_SIZE), this@CourseCardComponent)
    logoComponent.border = JBUI.Borders.emptyRight(CARD_GAP)

    add(logoComponent, BorderLayout.LINE_START)
    add(courseNameInfoComponent, BorderLayout.CENTER)

    preferredSize = JBUI.size(CARD_WIDTH, CARD_HEIGHT)

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

}

class CourseNameInfoComponent(course: Course, showOpenButton: Boolean) : JPanel(BorderLayout()) {
  private val nameComponent: CourseNameComponent = CourseNameComponent(course, showOpenButton)
  private val courseInfoComponent: JPanel

  init {
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

}

class CourseNameComponent(course: Course, showOpenButton: Boolean) : JPanel(BorderLayout()) {
  private val nameLabel: JLabel = JLabel()

  init {
    nameLabel.text = course.course.name
    nameLabel.font = Font(TypographyManager().bodyFont, Font.BOLD, CoursesDialogFontManager.fontSize)
    add(nameLabel, BorderLayout.CENTER)

    val coursePath = CoursesStorage.getInstance().getCoursePath(course)
    if (coursePath != null && showOpenButton) {
      val openCourseButton = OpenCourseButton()
      openCourseButton.addListener(CourseInfo(course))
      add(openCourseButton, BorderLayout.EAST)
    }
  }
}

class CommunityCourseInfoComponent(course: EduCourse) : JPanel(HorizontalLayout(INFO_HGAP)) {
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
    rating.font = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.smallCardFontSize)

    downloads.foreground = GRAY_COLOR
    downloads.icon = EducationalCoreIcons.User
    downloads.text = course.learnersCount.toString()
    downloads.border = JBUI.Borders.emptyRight(H_GAP)
    downloads.font = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.smallCardFontSize)

    val authors = course.authorFullNames.joinToString()
    authorComponent.isVisible = authors.isNotEmpty()
    authorComponent.foreground = GRAY_COLOR
    authorComponent.font = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.smallCardFontSize)
    if (authorComponent.isVisible) {
      authorComponent.text = authors
    }

    add(rating)
    add(downloads)
    add(authorComponent)
    border = JBUI.Borders.emptyBottom(5)
  }
}

class AcademyCourseInfoComponent(course: Course?) : JPanel(FlowLayout(FlowLayout.LEFT, INFO_HGAP, INFO_VGAP)) {
  private val commentLabel = JLabel()

  init {
    commentLabel.foreground = GRAY_COLOR
    commentLabel.font = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.smallCardFontSize)

    if (course is JetBrainsAcademyCourse) {
      commentLabel.text = ACADEMY_TEXT
    }
    add(commentLabel)
  }
}

class MyCourseInfoComponent(courseMetaInfo: CourseMetaInfo) : JPanel(FlowLayout(FlowLayout.LEFT, INFO_HGAP, INFO_VGAP)) {
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


