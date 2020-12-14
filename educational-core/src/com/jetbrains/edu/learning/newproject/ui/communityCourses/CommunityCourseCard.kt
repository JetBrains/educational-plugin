package com.jetbrains.edu.learning.newproject.ui.communityCourses

import com.intellij.icons.AllIcons
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesDialogFontManager
import com.jetbrains.edu.learning.newproject.ui.GRAY_COLOR
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import icons.EducationalCoreIcons
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel

private const val H_GAP = 10
private const val INFO_HGAP = 0

class CommunityCourseCard(course: Course) : CourseCardComponent(course) {

  override fun createCourseInfoComponent(): JPanel {
    return CommunityCourseInfoComponent(course as EduCourse)
  }
}

private class CommunityCourseInfoComponent(course: EduCourse) : JPanel(HorizontalLayout(INFO_HGAP)) {
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

    val authors: String = if (course.isMarketplace) {
      course.vendor.name
    }
    else {
      course.authorFullNames.joinToString()
    }
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