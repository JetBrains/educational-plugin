package com.jetbrains.edu.learning.newproject.ui.coursePanel


import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.plugins.newui.ColorButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.ui.JBColor
import com.intellij.util.NotNullProducer
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.joinJetBrainsAcademy
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectOpener
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.awt.Color
import java.awt.event.ActionListener

// TODO: use this everywhere in browse courses
val MAIN_BG_COLOR: Color = JBColor.namedColor("BrowseCourses.background", JBColor(
  (NotNullProducer { if (JBColor.isBright()) TaskDescriptionView.getTaskDescriptionBackgroundColor() else Color(0x313335) })))
private val WhiteForeground: Color = JBColor(Color.white, Color(0xBBBBBB))
private val GreenColor: Color = JBColor(0x5D9B47, 0x2B7B50)
private val FillForegroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installFillForeground", WhiteForeground)
private val FillBackgroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installFillBackground", GreenColor)
private val ForegroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installForeground", GreenColor)
private val BackgroundColor: Color = JBColor.namedColor("BrowseCourses.Button.installBackground", MAIN_BG_COLOR)
private val FocusedBackground: Color = JBColor.namedColor("EBrowseCourses.Button.installFocusedBackground", Color(0xE1F6DA))
private val BorderColor: Color = JBColor.namedColor("BrowseCourses.Button.installBorderColor", GreenColor)


class OpenCourseButton(coursePath: String) : CourseButtonBase() {

  init {
    text = "Open"
    setWidth72(this)

    addActionListener {
      ApplicationManager.getApplication().invokeAndWait {
        val project = ProjectUtil.openProject(coursePath, null, true)
        ProjectUtil.focusProjectWindow(project, true)
      }
    }
  }
}

class JBAcademyCourseButton(course: JetBrainsAcademyCourse, fill: Boolean = true) : CourseButtonBase(fill) {

  init {
    text = if (HyperskillSettings.INSTANCE.account == null) "Log In" else "Start"
    setWidth72(this)

    addActionListener {
      if (HyperskillSettings.INSTANCE.account == null) {
        HyperskillConnector.getInstance().doAuthorize(
          Runnable { runInEdt(ModalityState.stateForComponent(this)) { HyperskillProjectOpener.requestFocus() } },
          Runnable { text = "Start" }
        )
      }
      else {
        joinJetBrainsAcademy() {}  // TODO: error handling
      }
    }
  }
}

class StartCourseButton(fill: Boolean = true, joinCourse: (CourseInfo, CourseMode) -> Unit) : StartCourseButtonBase(joinCourse, fill) {
  override val courseMode = CourseMode.STUDY

  init {
    text = "Start"
    setWidth72(this)
  }

  override fun isVisible(course: Course): Boolean = true

}

class EditCourseButton(joinCourse: (CourseInfo, CourseMode) -> Unit) : StartCourseButtonBase(joinCourse) {
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
abstract class StartCourseButtonBase(private val joinCourse: (CourseInfo, CourseMode) -> Unit,
                                     fill: Boolean = false) : CourseButtonBase(fill) {
  private var listener: ActionListener? = null
  abstract val courseMode: CourseMode

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

abstract class CourseButtonBase(fill: Boolean = false) : ColorButton() {
  init {
    setTextColor(if (fill) FillForegroundColor else ForegroundColor)
    setBgColor(if (fill) FillBackgroundColor else BackgroundColor)
    setFocusedBgColor(FocusedBackground)
    setBorderColor(BorderColor)
    setFocusedBorderColor(BorderColor)
    setFocusedTextColor(ForegroundColor)
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

