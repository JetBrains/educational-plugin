package com.jetbrains.edu.learning.newproject.ui.coursePanel


import com.intellij.ide.plugins.newui.ColorButton
import com.intellij.ui.JBColor
import com.intellij.util.NotNullProducer
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.joinCourse
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import java.awt.Color
import java.awt.event.ActionListener


private val MAIN_BG_COLOR: Color = JBColor.namedColor("Edu.CourseDialog.background", JBColor(
  NotNullProducer { if (JBColor.isBright()) UIUtil.getListBackground() else Color(0x313335) }))
private val WhiteForeground: Color = JBColor(Color.white, Color(0xBBBBBB))
private val GreenColor: Color = JBColor(0x5D9B47, 0x2B7B50)
private val FillForegroundColor: Color = JBColor.namedColor("Edu.CourseDialog.Button.installFillForeground", WhiteForeground)
private val FillBackgroundColor: Color = JBColor.namedColor("Edu.CourseDialog.Button.installFillBackground", GreenColor)
private val ForegroundColor: Color = JBColor.namedColor("Edu.CourseDialog.Button.installForeground", GreenColor)
private val BackgroundColor: Color = JBColor.namedColor("Edu.CourseDialog.Button.installBackground", MAIN_BG_COLOR)
private val FocusedBackground: Color = JBColor.namedColor("Edu.CourseDialog.Button.installFocusedBackground", Color(0xE1F6DA))
private val BorderColor: Color = JBColor.namedColor("Edu.CourseDialog.Button.installBorderColor", GreenColor)


class StartCourseButton(errorHandler: (ErrorState) -> Unit) : StartCourseButtonBase(errorHandler) {
  override val courseMode = EduNames.STUDY

  init {
    text = "Start"
    setTextColor(FillForegroundColor)
    setBgColor(FillBackgroundColor)
    setWidth72(this)
  }

  override fun isVisible(course: Course): Boolean = true

}

class EditCourseButton(errorHandler: (ErrorState) -> Unit) : StartCourseButtonBase(errorHandler) {
  override val courseMode = CCUtils.COURSE_MODE

  init {
    text = "Edit"
    setTextColor(ForegroundColor)
    setFocusedTextColor(ForegroundColor)
    setBgColor(BackgroundColor)
    setWidth72(this)
  }

  override fun isVisible(course: Course) = false

}

/**
 * inspired by [com.intellij.ide.plugins.newui.InstallButton]
 */
abstract class StartCourseButtonBase(private val errorHandler: (ErrorState) -> Unit) : ColorButton() {
  private var listener: ActionListener? = null
  abstract val courseMode: String

  init {
    setFocusedBgColor(FocusedBackground)
    setBorderColor(BorderColor)
    setFocusedBorderColor(BorderColor)
  }

  private fun actionListener(courseInfo: CourseInfo) = ActionListener {
    joinCourse(courseInfo, courseMode, errorHandler)
  }

  fun update(courseInfo: CourseInfo) {
    listener?.let { removeActionListener(listener) }
    isVisible = isVisible(courseInfo.course)
    if (isVisible) {
      addListener(courseInfo)
    }
  }

  abstract fun isVisible(course: Course): Boolean

  private fun addListener(courseInfo: CourseInfo) {
    listener = actionListener(courseInfo)
    addActionListener(listener)
  }
}

