package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.FilterComponent
import com.intellij.ui.Gray
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettings
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.*
import javax.swing.JPanel
import javax.swing.event.DocumentListener

const val DESCRIPTION_AND_SETTINGS_TOP_OFFSET = 25

private const val ERROR_LABEL_TOP_GAP = 20
private const val HORIZONTAL_MARGIN = 10
private const val LARGE_HORIZONTAL_MARGIN = 15
private const val LINE_BORDER_THICKNESS = 1
private val PANEL_SIZE = JBUI.size(400, 600)

// TODO: Rename to CoursePanel after CoursePanel.java is removed
class NewCoursePanel(
  val isStandalonePanel: Boolean,
  val isLocationFieldNeeded: Boolean,
  joinCourseAction: (CourseInfo, CourseMode) -> Unit
) : JPanel() {
  var errorState: ErrorState = ErrorState.NothingSelected
  var course: Course? = null

  private var header = HeaderPanel(leftMargin, joinCourseAction)
  private var description = CourseDescriptionPanel(leftMargin)
  private var advancedSettings = CourseSettings(isLocationFieldNeeded, leftMargin)
  private val errorLabel: HyperlinkLabel = HyperlinkLabel().apply { isVisible = false }
  private var mySearchField: FilterComponent? = null
  private val listeners: MutableList<CoursesPanel.CourseValidationListener> = ArrayList()

  val locationString: String?
    get() = advancedSettings.locationString

  val projectSettings: Any?
    get() = advancedSettings.getProjectSettings()

  private val leftMargin: Int
    get() {
      return if (isStandalonePanel) {
        LARGE_HORIZONTAL_MARGIN
      }
      else {
        HORIZONTAL_MARGIN
      }
    }

  init {
    layout = VerticalFlowLayout(0, 0)

    border = JBUI.Borders.customLine(DIVIDER_COLOR, 0, 0, 0, 0)

    add(header)
    add(description)
    add(advancedSettings)
    add(createErrorPanel())

    background = UIUtil.getEditorPaneBackground()
  }

  fun setButtonsEnabled(isEnabled: Boolean) {
    header.setButtonsEnabled(isEnabled)
  }

  override fun getMinimumSize(): Dimension {
    return PANEL_SIZE
  }

  fun addLocationFieldDocumentListener(listener: DocumentListener) {
    advancedSettings.addLocationFieldDocumentListener(listener)
  }

  private fun updateCourseDescriptionPanel(course: Course, settings: CourseDisplaySettings = CourseDisplaySettings()) {
    val location = locationString
    if (location == null && isLocationFieldNeeded) {
      // TODO: set error
      return
    }
    header.update(CourseInfo(course, { locationString }, { projectSettings }), settings)
    description.bind(course)
  }

  private fun createErrorPanel(): JPanel {
    val errorPanel = JPanel(BorderLayout())
    errorPanel.add(errorLabel, BorderLayout.CENTER)
    errorPanel.border = JBUI.Borders.empty(ERROR_LABEL_TOP_GAP, leftMargin, 0, 0)
    addErrorStateListener()
    return errorPanel
  }

  private fun addErrorStateListener() {
    errorLabel.addHyperlinkListener(ErrorStateHyperlinkListener())
  }

  fun bindCourse(course: Course, settings: CourseDisplaySettings = CourseDisplaySettings()): LanguageSettings<*>? {
    this.course = course
    advancedSettings.update(course, settings.showLanguageSettings)
    updateCourseDescriptionPanel(course, settings)
    revalidate()
    repaint()
    return advancedSettings.languageSettings
  }

  fun notifyListeners(canStartCourse: Boolean) {
    for (listener in listeners) {
      listener.validationStatusChanged(canStartCourse)
    }
  }

  fun addCourseValidationListener(listener: CoursesPanel.CourseValidationListener) {
    listeners.add(listener)
    listener.validationStatusChanged(canStartCourse())
  }

  fun validateSettings(course: Course?) = advancedSettings.validateSettings(course)

  fun bindSearchField(searchField: FilterComponent) {
    mySearchField = searchField
  }

  fun hideErrorPanel() {
    errorLabel.isVisible = false
  }

  fun setError(errorState: ErrorState) {
    this.errorState = errorState
    val message = errorState.message
    header.setButtonToolTip(null)
    if (message != null) {
      when (errorState) {
        is ErrorState.JetBrainsAcademyLoginNeeded -> {
          errorLabel.isVisible = true
          errorLabel.setHyperlinkText(message.beforeLink, message.linkText, message.afterLink)
          header.setButtonToolTip(EduCoreBundle.message("course.dialog.login.required"))
        }
        else -> {
          errorLabel.isVisible = true
          errorLabel.setHyperlinkText(message.beforeLink, message.linkText, message.afterLink)
          header.setButtonToolTip(message.beforeLink + message.linkText + message.afterLink)
        }
      }
    }
    else {
      errorLabel.isVisible = false
    }
    errorLabel.foreground = errorState.foregroundColor
  }

  private fun canStartCourse(): Boolean = errorState.courseCanBeStarted

  companion object {
    // default divider's color too dark in Darcula, so use the same color as in plugins dialog
    val DIVIDER_COLOR = JBColor(Gray._192, Gray._81)
  }
}
