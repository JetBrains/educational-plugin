package com.jetbrains.edu.learning.newproject.ui.coursePanel


import com.intellij.ide.plugins.newui.ColorButton
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.JBColor
import com.intellij.util.NotNullProducer
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.codeforces.actions.StartCodeforcesContestAction
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.isPreview
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.marketplace.MarketplaceListedCoursesIdsLoader
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseMetaInfo
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.newproject.ui.getColorFromScheme
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStageRequest
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import java.awt.Color
import java.awt.event.ActionListener

val MAIN_BG_COLOR: Color
  get() = JBColor.namedColor(
    "BrowseCourses.background", JBColor(
    (NotNullProducer { if (JBColor.isBright()) TaskDescriptionView.getTaskDescriptionBackgroundColor() else Color(0x313335) })
  )
  )
private val WhiteForeground: Color = JBColor(Color.white, Color(0xBBBBBB))
private val GreenColor: Color = JBColor(0x5D9B47, 0x2B7B50)
private val FillForegroundColor: Color = JBColor.namedColor("BrowseCourses.Button.startFillForeground",
                                                            getColorFromScheme("Plugins.Button.installFillForeground", WhiteForeground))

private val FillBackgroundColor: Color = JBColor.namedColor("BrowseCourses.Button.startFillBackground",
                                                            getColorFromScheme("Plugins.Button.installFillBackground", GreenColor))
private val ForegroundColor: Color = JBColor.namedColor("BrowseCourses.Button.startForeground",
                                                        getColorFromScheme("Plugins.Button.installForeground", GreenColor))
private val BackgroundColor: Color = JBColor.namedColor("BrowseCourses.Button.startBackground", MAIN_BG_COLOR)
private val FocusedBackground: Color = JBColor.namedColor("BrowseCourses.Button.startFocusedBackground", Color(0xE1F6DA))
private val BorderColor: Color = JBColor.namedColor("BrowseCourses.Button.border",
                                                    getColorFromScheme("Plugins.Button.installBorderColor", GreenColor))

class OpenCourseButton : CourseButtonBase() {

  override fun actionListener(course: Course): ActionListener = ActionListener {
    invokeLater {
      val coursesStorage = CoursesStorage.getInstance()
      val coursePath = coursesStorage.getCoursePath(course) ?: return@invokeLater
      if (!FileUtil.exists(coursePath)) {
        processMissingCourseOpening(course, coursePath)
        return@invokeLater
      }

      closeDialog()
      course.openCourse()
    }
  }

  private fun processMissingCourseOpening(course: Course, coursePath: String) {
    val isFromMyCoursesPage = course is CourseMetaInfo
    val message = if (isFromMyCoursesPage) {
      EduCoreBundle.message("course.dialog.my.courses.remove.course")
    }
    else {
      EduCoreBundle.message("course.dialog.course.not.found.reopen.button")
    }

    if (showNoCourseDialog(coursePath, message) == Messages.NO) {
      CoursesStorage.getInstance().removeCourseByLocation(coursePath)
      when {
        isFromMyCoursesPage -> {
          return
        }
        course is HyperskillCourse -> {
          closeDialog()
          ProjectOpener.getInstance().apply {
            HyperskillOpenInIdeRequestHandler.openInNewProject(HyperskillOpenStageRequest(course.id, null)).onError {
              Messages.showErrorDialog(it.message, EduCoreBundle.message("course.dialog.error.restart.jba"))
              logger<HyperskillOpenInIdeRequestHandler>().warn("Opening a new project resulted in an error: ${it.message}. The error was shown inside an error dialog.")
            }
          }
        }
        course is CodeforcesCourse -> {
          closeDialog()
          val contestId = course.id
          StartCodeforcesContestAction.joinContest(contestId, null)
        }
        else -> {
          closeDialog()
          // if course is present both on stepik and marketplace we open marketplace-based one
          val marketplaceId = MarketplaceListedCoursesIdsLoader.getMarketplaceIdByStepikId(course.id)
          val courseToOpen = if (marketplaceId != null) {
            MarketplaceConnector.getInstance().searchCourse(marketplaceId, course.isMarketplacePrivate) ?: course
          }
          else {
            course
          }
          JoinCourseDialog(courseToOpen).show()
        }
      }
    }
  }

  private fun closeDialog() {
    val dialog = UIUtil.getParentOfType(DialogWrapperDialog::class.java, this) ?: error("Dialog is null")
    dialog.dialogWrapper?.close(DialogWrapper.CANCEL_EXIT_CODE)
  }

  override fun isVisible(course: Course): Boolean = !course.isPreview && CoursesStorage.getInstance().hasCourse(course)
}

/**
 * inspired by [com.intellij.ide.plugins.newui.InstallButton]
 */
class StartCourseButton(
  private val joinCourse: (Course, CourseMode) -> Unit,
  fill: Boolean = false
) : CourseButtonBase(fill) {

  override fun actionListener(course: Course) = ActionListener {
    joinCourse(course, CourseMode.STUDENT)
  }

  override fun isVisible(course: Course): Boolean {
    if (!course.isPreview && CoursesStorage.getInstance().hasCourse(course)) {
      return false
    }

    if (course is CodeforcesCourse) {
      val isRegistrationPossible = course.isRegistrationOpen
                                   && course.isUpcomingContest
                                   && !CodeforcesConnector.getInstance().isUserRegisteredForContest(course.id)
      return isRegistrationPossible || course.isOngoing || course.isPastContest
    }

    return true
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

  protected abstract fun actionListener(course: Course): ActionListener

  open fun update(course: Course) {
    isVisible = isVisible(course)
    addListener(course)
  }

  private fun addListener(course: Course) {
    listener?.let { removeActionListener(listener) }
    isVisible = isVisible(course.course)
    if (isVisible) {
      listener = actionListener(course)
      addActionListener(listener)
    }
  }
}
