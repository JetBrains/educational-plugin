package com.jetbrains.edu.learning.newproject.ui.coursePanel


import com.intellij.ide.plugins.newui.ColorButton
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.JBColor
import com.intellij.util.NotNullProducer
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.ProjectOpener
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplacePlatformProvider.Companion.stepikMarketplaceIdsMap
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CourseMetaInfo
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialog
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.getColorFromScheme
import com.jetbrains.edu.learning.newproject.ui.getErrorState
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

  init {
    text = "Open"
    setWidth72(this)
  }

  override fun actionListener(courseInfo: CourseInfo): ActionListener = ActionListener {
    invokeLater {
      val course = courseInfo.course
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
              Messages.showErrorDialog(it, EduCoreBundle.message("course.dialog.error.restart.jba"))
            }
          }
        }
        else -> {
          closeDialog()
          // if course is present both on stepik and marketplace we open marketplace-based one
          val marketplaceId = stepikMarketplaceIdsMap[course.id]
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

  override fun isVisible(course: Course): Boolean = course.dataHolder.getUserData(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW_KEY) != true
                                                    && course.dataHolder.getUserData(
    CCCreateCoursePreviewDialog.IS_LOCAL_COURSE_KEY) != true
                                                    && CoursesStorage.getInstance().hasCourse(course)
}

class StartCourseButton(joinCourse: (CourseInfo, CourseMode) -> Unit, fill: Boolean = true) : StartCourseButtonBase(joinCourse, fill) {
  override val courseMode = CourseMode.STUDY

  init {
    text = "Start"
    setWidth72(this)
  }

  override fun isVisible(course: Course): Boolean =
    course.dataHolder.getUserData(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW_KEY) == true
    || course.dataHolder.getUserData(CCCreateCoursePreviewDialog.IS_LOCAL_COURSE_KEY) == true
    || !CoursesStorage.getInstance().hasCourse(course)

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
                                           && CoursesStorage.getInstance().getCourseMetaInfo(course) == null
                                           && course.dataHolder.getUserData(CCCreateCoursePreviewDialog.IS_LOCAL_COURSE_KEY) != true
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
    isEnabled = canStartCourse(courseInfo)
    addListener(courseInfo)
  }

  fun addListener(courseInfo: CourseInfo) {
    listener?.let { removeActionListener(listener) }
    isVisible = isVisible(courseInfo.course)
    if (isVisible) {
      listener = actionListener(courseInfo)
      addActionListener(listener)
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
