package com.jetbrains.edu.learning.newproject.ui.coursePanel


import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.plugins.newui.ColorButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.JBColor
import com.intellij.util.NotNullProducer
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.CoursesStorage
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.getErrorState
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.awt.Color
import java.awt.event.ActionListener

val MAIN_BG_COLOR: Color
  get() = JBColor.namedColor("BrowseCourses.background", JBColor(
    (NotNullProducer { if (JBColor.isBright()) TaskDescriptionView.getTaskDescriptionBackgroundColor() else Color(0x313335) })))
private val WhiteForeground: Color = JBColor(Color.white, Color(0xBBBBBB))
private val GreenColor: Color = JBColor(0x5D9B47, 0x2B7B50)
private val FillForegroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installFillForeground", WhiteForeground)
private val FillBackgroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installFillBackground", GreenColor)
private val ForegroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installForeground", GreenColor)
private val BackgroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installBackground", MAIN_BG_COLOR)
private val FocusedBackground: Color = JBColor.namedColor("EBrowseCourses.Button.installFocusedBackground", Color(0xE1F6DA))
private val BorderColor: Color = JBColor.namedColor("BrowseCourses.Button.installBorderColor", GreenColor)

class OpenCourseButton : CourseButtonBase() {

  init {
    text = "Open"
    setWidth72(this)
  }

  override fun actionListener(courseInfo: CourseInfo): ActionListener = ActionListener {
    ApplicationManager.getApplication().invokeAndWait {
      val coursePath = CoursesStorage.getInstance().getCoursePath(courseInfo.course) ?: return@invokeAndWait
      val project = ProjectUtil.openProject(coursePath, null, true)
      ProjectUtil.focusProjectWindow(project, true)
    }
  }

  override fun isVisible(course: Course): Boolean = course.getUserData(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW_KEY) != true
                                                    && CoursesStorage.getInstance().getCoursePath(course) != null
}

class StartCourseButton(joinCourse: (CourseInfo, CourseMode) -> Unit, fill: Boolean = true) : StartCourseButtonBase(joinCourse, fill) {
  override val courseMode = CourseMode.STUDY

  init {
    text = "Start"
    setWidth72(this)
  }

  // we place this button on course card and course panel both
  override fun isVisible(course: Course): Boolean = course.getUserData(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW_KEY) == true
                                                    || CoursesStorage.getInstance().getCoursePath(course) == null

  override fun canStartCourse(courseInfo: CourseInfo) = courseInfo.projectSettings != null
                                                        && courseInfo.location() != null
                                                        && getErrorState(courseInfo.course) {
    validateSettings(courseInfo)
  }.courseCanBeStarted

  private fun validateSettings(courseInfo: CourseInfo): ValidationMessage? {
    val languageSettings = courseInfo.languageSettings()
    return languageSettings?.validate(courseInfo.course, courseInfo.location())
  }

}

class EditCourseButton(errorHandler: (CourseInfo, CourseMode) -> Unit) : StartCourseButtonBase(errorHandler) {
  override val courseMode = CourseMode.COURSE_CREATOR

  init {
    text = "Edit"
    setWidth72(this)
  }

  override fun isVisible(course: Course) = course.isViewAsEducatorEnabled
}

/**
 * inspired by [com.intellij.ide.plugins.newui.InstallButton]
 */
abstract class StartCourseButtonBase(
  private val joinCourse: (CourseInfo, CourseMode) -> Unit,
  fill: Boolean = false
) : CourseButtonBase(fill) {
  abstract val courseMode: CourseMode

  override fun actionListener(courseInfo: CourseInfo) = ActionListener {
    joinCourse(courseInfo, courseMode)
  }

}

abstract class CourseButtonBase(fill: Boolean = false) : ColorButton() {
  private var listener: ActionListener? = null

  init {
    setTextColor(if (fill) FillForegroundColor else ForegroundColor)
    setBgColor(if (fill) FillBackgroundColor else BackgroundColor)
    setFocusedBgColor(FocusedBackground)
    setBorderColor(BorderColor)
    setFocusedBorderColor(BorderColor)
    setFocusedTextColor(ForegroundColor)
  }

  abstract fun isVisible(course: Course): Boolean

  open fun canStartCourse(courseInfo: CourseInfo): Boolean = true

  protected abstract fun actionListener(courseInfo: CourseInfo): ActionListener

  open fun update(courseInfo: CourseInfo) {
    isVisible = isVisible(courseInfo.course)
    addListener(courseInfo)
  }

  fun addListener(courseInfo: CourseInfo) {
    listener?.let { removeActionListener(listener) }
    isVisible = isVisible(courseInfo.course)
    if (isVisible) {
      addActionListener(actionListener(courseInfo))
    }
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

