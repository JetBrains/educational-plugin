package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesDialogFontManager
import com.jetbrains.edu.learning.newproject.ui.GRAY_COLOR
import com.jetbrains.edu.learning.newproject.ui.createCourseDescriptionStylesheet
import com.jetbrains.edu.learning.stepik.ListedCoursesIdsProvider
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import icons.EducationalCoreIcons
import java.awt.BorderLayout
import java.awt.Font
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*


// TODO: remove when new UI is implemented
private const val NOT_VERIFIED_NOTE = """
  
  
                                      Note: This course has not been verified by the JetBrains team.
                                      If you want to join it, keep in mind we are not responsible for the content provided.
                                      If you are the owner of the course and you want it to be 
                                      featured, please <a href="mailto:academy@jetbrains.com">get in touch with us</a>
                                      and we would be glad to verify it with you.
                                      """

class CourseDetailsPanel(leftMargin: Int) : NonOpaquePanel(VerticalFlowLayout(0, 5)), CourseSelectionListener {
  @Suppress("DialogTitleCapitalization")
  private val courseDetailsHeader: JBLabel = JBLabel(EduCoreBundle.message("course.dialog.course.details")).apply {
    font = JBFont.create(Font("SF UI Text", Font.BOLD, JBUI.scaleFontSize(15.0f)), true)
  }

  private var courseStatisticsPanel: CourseStatisticsPanel = CourseStatisticsPanel()

  private val descriptionPanel: CourseDescriptionHtmlPanel = CourseDescriptionHtmlPanel().apply {
    background = MAIN_BG_COLOR
  }

  init {
    border = JBUI.Borders.empty(DESCRIPTION_AND_SETTINGS_TOP_OFFSET, leftMargin, 0, 0)

    add(courseDetailsHeader, BorderLayout.PAGE_START)
    add(courseStatisticsPanel)
    add(JBScrollPane(descriptionPanel).apply { border = null }, BorderLayout.CENTER)
  }

  override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
    val course = courseInfo.course
    courseDetailsHeader.isVisible = !course.description.isNullOrEmpty()
    if (course is EduCourse) {
      courseStatisticsPanel.setStatistics(course)
      courseStatisticsPanel.isVisible = true
    }
    else {
      courseStatisticsPanel.isVisible = false
    }
    descriptionPanel.bind(course)
    revalidate()
    repaint()
  }

  companion object {
    const val DATE_PATTERN: String = "MMM d, yyyy"

    fun formatNumber(number: Int): String {
      return NumberFormat.getNumberInstance(Locale.US).format(number)
    }

  }
}

private class CourseDescriptionHtmlPanel : CourseHtmlPanel() {

  override fun getBody(): String {
    course?.let {
      var description = it.description ?: ""
      if (it.needsVerification) {
        description += NOT_VERIFIED_NOTE
      }
      return description.replace("\n", "<br>")
    }

    return ""
  }

  override fun setBody(text: String) {
    if (text.isEmpty()) {
      setText("")
    }
    else {
      setText("""
        <html>
        <head>
          <style>
            ${createCourseDescriptionStylesheet()}
          </style>
        </head>
        <body>
        $text
        </body>
        </html>
      """.trimIndent())
    }
  }

  override fun getBodyFont(): Font = Font(
    TypographyManager().bodyFont,
    Font.PLAIN,
    JBUI.scaleFontSize(EditorColorsManager.getInstance().globalScheme.editorFontSize.toFloat()))


  private val Course.needsVerification: Boolean
    get() {
      return this is EduCourse
             && isStepikRemote
             && isStepikPublic
             && !ListedCoursesIdsProvider.featuredCommunityCourses.contains(id)
             && this !is StepikCourse
             && !ListedCoursesIdsProvider.inProgressCourses.contains(id)
    }
}

private class CourseStatisticsPanel : NonOpaquePanel(HorizontalLayout(0)) {
  private val rating: JBLabel
  private val learners: JBLabel
  private val date: JBLabel
  private val lastDotLabel = createDotLabel()

  init {
    val componentsFont = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.smallCardFontSize)
    border = JBUI.Borders.empty(8, 0)

    rating = createGrayTextLabel(componentsFont)
    learners = createGrayTextLabel(componentsFont)
    date = createGrayTextLabel(componentsFont)

    add(rating)
    add(createDotLabel())
    add(learners)
    add(lastDotLabel)
    add(date)
  }

  private fun createDotLabel() = JBLabel(EducationalCoreIcons.DOT).apply { border = JBUI.Borders.emptyRight(6) }

  private fun createGrayTextLabel(componentsFont: Font): JBLabel {
    return JBLabel().apply {
      foreground = GRAY_COLOR
      border = JBUI.Borders.emptyRight(6)
      font = componentsFont
    }
  }

  fun setStatistics(course: EduCourse) {
    rating.icon = AllIcons.Plugins.Rating
    if (course.reviewScore != 0.0) {
      rating.text = "%.${1}f".format(course.reviewScore)
    }
    else {
      rating.text = EduCoreBundle.message("course.dialog.card.not.rated")
    }

    learners.isVisible = course.learnersCount != 0
    learners.text = if (course.learnersCount == 1) {
      EduCoreBundle.message("course.dialog.course.stats.one.learner")
    }
    else {
      val learnersCount = CourseDetailsPanel.formatNumber(course.learnersCount)
      EduCoreBundle.message("course.dialog.course.stats.learners", learnersCount)
    }

    lastDotLabel.isVisible = course.updateDate != Date(0)
    date.isVisible = course.updateDate != Date(0)
    date.text = EduCoreBundle.message("course.dialog.updated",
                                      SimpleDateFormat(CourseDetailsPanel.DATE_PATTERN, Locale.US).format(course.updateDate))
  }
}


