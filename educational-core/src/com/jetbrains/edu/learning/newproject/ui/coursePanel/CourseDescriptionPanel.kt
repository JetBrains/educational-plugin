package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.newproject.ui.createCourseDescriptionStylesheet
import com.jetbrains.edu.learning.stepik.ListedCoursesIdsProvider
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.TypographyManager
import java.awt.Font


// TODO: remove when new UI is implemented
private const val NOT_VERIFIED_NOTE = """
  
  
                                      Note: This course has not been verified by the JetBrains team.
                                      If you want to join it, keep in mind we are not responsible for the content provided.
                                      If you are the owner of the course and you want it to be 
                                      featured, please <a href="mailto:academy@jetbrains.com">get in touch with us</a>
                                      and we would be glad to verify it with you.
                                      """

class CourseDescriptionPanel(leftMargin: Int) : JBScrollPane() {
  private val descriptionPanel: CourseDescriptionHtmlPanel

  init {
    border = JBUI.Borders.emptyLeft(leftMargin)
    descriptionPanel = CourseDescriptionHtmlPanel()
    descriptionPanel.border = JBUI.Borders.emptyTop(DESCRIPTION_AND_SETTINGS_TOP_OFFSET)
    descriptionPanel.background = MAIN_BG_COLOR
    setViewportView(descriptionPanel)
  }

  fun bind(course: Course) {
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
             && isRemote
             && isPublic
             && !ListedCoursesIdsProvider.featuredCommunityCourses.contains(id)
             && this !is StepikCourse
             && !ListedCoursesIdsProvider.inProgressCourses.contains(id)
    }
}


