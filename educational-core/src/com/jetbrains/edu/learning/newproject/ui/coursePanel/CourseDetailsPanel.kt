package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.createCourseDescriptionStylesheet
import com.jetbrains.edu.learning.stepik.ListedCoursesIdsProvider
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import java.awt.BorderLayout
import java.awt.Font


// TODO: remove when new UI is implemented
private const val NOT_VERIFIED_NOTE = """
  
  
                                      Note: This course has not been verified by the JetBrains team.
                                      If you want to join it, keep in mind we are not responsible for the content provided.
                                      If you are the owner of the course and you want it to be 
                                      featured, please <a href="mailto:academy@jetbrains.com">get in touch with us</a>
                                      and we would be glad to verify it with you.
                                      """

class CourseDetailsPanel(leftMargin: Int) : NonOpaquePanel(VerticalFlowLayout(0, 5)), CourseSelectionListener {

  private val descriptionPanel: CourseDescriptionHtmlPanel = CourseDescriptionHtmlPanel().apply {
    background = MAIN_BG_COLOR
  }

  @Suppress("DialogTitleCapitalization")
  private val courseDetailsHeader: JBLabel = JBLabel(EduCoreBundle.message("course.dialog.course.details")).apply {
    font = JBFont.create(Font("SF UI Text", Font.BOLD, JBUI.scaleFontSize(15.0f)), true)
  }

  init {
    border = JBUI.Borders.empty(DESCRIPTION_AND_SETTINGS_TOP_OFFSET, leftMargin, 0, 0)

    add(courseDetailsHeader, BorderLayout.PAGE_START)
    val jbScrollPane = JBScrollPane(descriptionPanel).apply {
      border = null
    }
    add(jbScrollPane, BorderLayout.CENTER)
  }

  override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
    val course = courseInfo.course
    courseDetailsHeader.isVisible = !course.description.isNullOrEmpty()
    descriptionPanel.bind(course)
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


