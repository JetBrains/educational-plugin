package com.jetbrains.edu.learning.newproject.ui.coursePanel


import com.intellij.ide.BrowserUtil
import com.intellij.ide.impl.ProjectUtil
import com.intellij.ide.plugins.newui.ColorButton
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.ui.JBColor
import com.intellij.util.NotNullProducer
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.CoursesStorage
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialogBase
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectAction
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

private const val BEFORE_LINK = "beforeLink"
private const val LINK = "link"
private const val LINK_TEXT = "linkText"
private const val AFTER_LINK = "afterLink"
private val LINK_ERROR_PATTERN: Regex = """(?<$BEFORE_LINK>.*)<a href="(?<$LINK>.*)">(?<$LINK_TEXT>.*)</a>(?<$AFTER_LINK>.*)""".toRegex()

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

class JBAcademyCourseButton(fill: Boolean = true, errorHandler: (ErrorState) -> Unit) : CourseButtonBase(fill) {

  init {
    text = "Start"
    setWidth72(this)

    addActionListener {
      // TODO: enable button if user not logged in
      if (HyperskillSettings.INSTANCE.account == null) {
        HyperskillConnector.getInstance().doAuthorize(
          Runnable { runInEdt(ModalityState.stateForComponent(this)) { HyperskillProjectOpener.requestFocus() } },
          Runnable { text = "Start" }
        )
      }
      else {
        joinJetBrainsAcademy(errorHandler)
      }
    }
  }

  private fun joinJetBrainsAcademy(setError: (ErrorState) -> Unit) {
    val account = HyperskillSettings.INSTANCE.account ?: return

    HyperskillProjectAction.openHyperskillProject(account) { errorMessage ->
      val groups = LINK_ERROR_PATTERN.matchEntire(errorMessage)?.groups
      val errorState = if (groups == null) ErrorState.CustomSevereError(errorMessage)
      else ErrorState.CustomSevereError(groups.valueOrEmpty(BEFORE_LINK),
                                        groups.valueOrEmpty(LINK_TEXT),
                                        groups.valueOrEmpty(AFTER_LINK),
                                        Runnable { BrowserUtil.browse(groups.valueOrEmpty(LINK)) })
      setError(errorState)
    }
  }
}

class StartCourseButton(fill: Boolean = true, errorHandler: (ErrorState) -> Unit) : StartCourseButtonBase(errorHandler, fill) {
  override val courseMode = CourseMode.STUDY

  init {
    text = "Start"
    setWidth72(this)
  }

  override fun isVisible(course: Course): Boolean = true

}

class EditCourseButton(errorHandler: (ErrorState) -> Unit) : StartCourseButtonBase(errorHandler) {
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
abstract class StartCourseButtonBase(private val errorHandler: (ErrorState) -> Unit, fill: Boolean = false) : CourseButtonBase(fill) {
  private var listener: ActionListener? = null
  abstract val courseMode: CourseMode

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
    listener?.apply { removeActionListener(listener) }
    listener = actionListener(courseInfo)
    addActionListener(listener)
  }

  private fun joinCourse(courseInfo: CourseInfo, courseMode: CourseMode, setError: (ErrorState) -> Unit) {
    val (course, getLocation, getProjectSettings) = courseInfo

    // location is null for course preview dialog only
    val location = getLocation()
    if (location == null) {
      return
    }

    CoursesStorage.getInstance().addCourse(course, location)

    val configurator = course.configurator
    // If `configurator != null` than `projectSettings` is always not null
    // because project settings are produced by configurator itself
    val projectSettings = getProjectSettings()
    if (configurator != null && projectSettings != null) {
      try {
        configurator.beforeCourseStarted(course)

      val dialog = UIUtil.getParentOfType(JoinCourseDialogBase::class.java, this)
      dialog?.close()
        course.courseMode = courseMode.toString()
        val projectGenerator = configurator
          .courseBuilder
          .getCourseProjectGenerator(course)
        projectGenerator?.doCreateCourseProject(location, projectSettings)
      }
      catch (e: CourseCantBeStartedException) {
        setError(e.error)
      }
    }
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

private fun MatchGroupCollection.valueOrEmpty(groupName: String): String = this[groupName]?.value ?: ""

