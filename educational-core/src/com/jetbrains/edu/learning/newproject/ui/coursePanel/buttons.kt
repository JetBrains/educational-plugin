package com.jetbrains.edu.learning.newproject.ui.coursePanel


import com.intellij.ide.plugins.newui.ColorButton
import com.intellij.ui.JBColor
import com.intellij.util.NotNullProducer
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import java.awt.Color
import java.awt.event.ActionListener


private val MAIN_BG_COLOR: Color = JBColor.namedColor("BrowseCourses.background", JBColor(
  (NotNullProducer { if (JBColor.isBright()) UIUtil.getListBackground() else Color(0x313335) })))
private val WhiteForeground: Color = JBColor(Color.white, Color(0xBBBBBB))
private val GreenColor: Color = JBColor(0x5D9B47, 0x2B7B50)
private val FillForegroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installFillForeground", WhiteForeground)
private val FillBackgroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installFillBackground", GreenColor)
private val ForegroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installForeground", GreenColor)
private val BackgroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installBackground", MAIN_BG_COLOR)
private val FocusedBackground: Color = JBColor.namedColor("EBrowseCourses.Button.installFocusedBackground", Color(0xE1F6DA))
private val BorderColor: Color = JBColor.namedColor("BrowseCourses.Button.installBorderColor", GreenColor)


// TODO: use proper button action and text. Problem: location and properties from info panel is needed
class OpenCourseButton(joinCourse: (CourseInfo, CourseMode) -> Unit) : StartCourseButtonBase(joinCourse) {
  override val courseMode = CourseMode.STUDY

  init {
    text = "Open"
    setTextColor(ForegroundColor)
    setFocusedTextColor(ForegroundColor)
    setBgColor(BackgroundColor)
    setWidth72(this)
  }

  override fun isVisible(course: Course): Boolean = true

  override fun isVisible(): Boolean = false  // TODO: remove working on open button
}

class StartCourseButton(joinCourse: (CourseInfo, CourseMode) -> Unit) : StartCourseButtonBase(joinCourse) {
  override val courseMode = CourseMode.STUDY

  init {
    text = "Start"
    setTextColor(FillForegroundColor)
    setBgColor(FillBackgroundColor)
    setWidth72(this)
  }

  override fun isVisible(course: Course): Boolean = true

}

class EditCourseButton(joinCourse: (CourseInfo, CourseMode) -> Unit) : StartCourseButtonBase(joinCourse) {
  override val courseMode = CourseMode.COURSE_CREATOR

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
abstract class StartCourseButtonBase(private val joinCourse: (CourseInfo, CourseMode) -> Unit) : ColorButton() {
  private var listener: ActionListener? = null
  abstract val courseMode: CourseMode

  init {
    setFocusedBgColor(FocusedBackground)
    setBorderColor(BorderColor)
    setFocusedBorderColor(BorderColor)
  }

  private fun actionListener(courseInfo: CourseInfo) = ActionListener {
    joinCourse(courseInfo, courseMode)
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
    listener?.apply { removeActionListener(listener) }
    listener = actionListener(courseInfo)
    addActionListener(listener)
  }
}

enum class CourseMode {
  STUDY {
    override fun toString(): String = EduNames.STUDY
  },
  COURSE_CREATOR {
    override fun toString(): String = CCUtils.COURSE_MODE
  };
}

