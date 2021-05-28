package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.ErrorComponent
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettingsPanel
import com.jetbrains.edu.learning.newproject.ui.getErrorState
import java.awt.CardLayout
import java.io.File
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
import javax.swing.event.DocumentEvent

const val DESCRIPTION_AND_SETTINGS_TOP_OFFSET = 25

private const val HORIZONTAL_MARGIN = 20
private const val ERROR_TOP_GAP = 27
private const val ERROR_BOTTOM_GAP = 1
private const val ERROR_LEFT_GAP = 5
private const val ERROR_RIGHT_GAP = 19
private const val ERROR_PANEL_MARGIN = 10

private const val EMPTY = "empty"
private const val CONTENT = "content"

abstract class CoursePanel(private val isLocationFieldNeeded: Boolean) : JPanel() {

  var errorState: ErrorState = ErrorState.NothingSelected
  var course: Course? = null

  private val header: HeaderPanel = HeaderPanel(HORIZONTAL_MARGIN) { courseInfo, courseMode -> joinCourse(courseInfo, courseMode) }
  private val description = CourseDescriptionPanel(HORIZONTAL_MARGIN)
  private val settings = CourseSettingsPanel(isLocationFieldNeeded, HORIZONTAL_MARGIN).apply { background = MAIN_BG_COLOR }
  private val errorComponent: ErrorComponent = ErrorComponent(ErrorStateHyperlinkListener(), ERROR_PANEL_MARGIN).apply {
    border = JBUI.Borders.empty(ERROR_TOP_GAP, HORIZONTAL_MARGIN + ERROR_LEFT_GAP, ERROR_BOTTOM_GAP, ERROR_RIGHT_GAP)
  }

  val locationString: String?
    get() = settings.locationString

  val projectSettings: Any?
    get() = settings.getProjectSettings()

  val languageSettings: LanguageSettings<*>?
    get() = settings.languageSettings

  init {
    layout = CardLayout()
    border = JBUI.Borders.customLine(DIVIDER_COLOR, 0, 0, 0, 0)

    val emptyStatePanel = JBPanelWithEmptyText().withEmptyText(EduCoreBundle.message("course.dialog.no.course.selected"))
    @Suppress("LeakingThis")
    add(emptyStatePanel, EMPTY)

    val content = JPanel(VerticalFlowLayout(0, 0))
    content.add(header)
    content.add(errorComponent)
    content.add(description)
    content.add(settings)
    content.background = MAIN_BG_COLOR

    val scrollPane = JBScrollPane(content, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER).apply {
      border = null
    }
    @Suppress("LeakingThis")
    add(scrollPane, CONTENT)
    setButtonsEnabled(canStartCourse())
  }

  private fun joinCourse(courseInfo: CourseInfo, courseMode: CourseMode) {
    val currentLocation = courseInfo.location()
    val locationErrorState = when {
      // if it's null it means there's no location field and it's ok
      currentLocation == null -> ErrorState.None
      currentLocation.isEmpty() -> ErrorState.EmptyLocation
      !FileUtil.ensureCanCreateFile(File(FileUtil.toSystemDependentName(currentLocation))) -> ErrorState.InvalidLocation
      else -> ErrorState.None
    }
    if (locationErrorState != ErrorState.None) {
      setError(locationErrorState)
    }
    else {
      joinCourseAction(courseInfo, courseMode)
    }
  }

  protected abstract fun joinCourseAction(info: CourseInfo, mode: CourseMode)

  private fun addOneTimeLocationFieldValidation() {
    settings.addLocationFieldDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        doValidation()
        settings.removeLocationFieldDocumentListener(this)
      }
    })
  }

  fun setButtonsEnabled(isEnabled: Boolean) {
    header.setButtonsEnabled(isEnabled)
  }

  fun showEmptyState() {
    (layout as CardLayout).show(this, EMPTY)
  }

  private fun updateCourseDescriptionPanel(course: Course, settings: CourseDisplaySettings = CourseDisplaySettings()) {
    val location = locationString
    if (location == null && isLocationFieldNeeded) {
      // TODO: set error
      return
    }
    header.update(CourseInfo(course, { locationString }, { this.settings.languageSettings }), settings)
    description.bind(course)
  }

  fun bindCourse(course: Course, settings: CourseDisplaySettings = CourseDisplaySettings()): LanguageSettings<*>? {
    (layout as CardLayout).show(this, CONTENT)
    this.course = course
    this.settings.update(course, settings.showLanguageSettings)
    updateCourseDescriptionPanel(course, settings)
    revalidate()
    repaint()
    doValidation()
    return this.settings.languageSettings
  }

  fun doValidation() {
    setError(getErrorState(course) { validateSettings(it) })
    setButtonsEnabled(errorState.courseCanBeStarted)
  }

  fun validateSettings(course: Course?) = settings.validateSettings(course)

  fun hideErrorPanel() {
    errorComponent.isVisible = false
    revalidate()
    repaint()
  }

  fun setError(errorState: ErrorState) {
    this.errorState = errorState
    setButtonsEnabled(errorState.courseCanBeStarted)
    header.setButtonToolTip(null)
    hideErrorPanel()

    showError(errorState)
  }

  protected open fun showError(errorState: ErrorState) {
    if (errorState is ErrorState.LocationError) {
      addOneTimeLocationFieldValidation()
    }

    val message = errorState.message ?: return
    when (errorState) {
      is ErrorState.JetBrainsAcademyLoginNeeded -> {
        errorComponent.setErrorMessage(message)
        header.setButtonToolTip(EduCoreBundle.message("course.dialog.login.required"))
      }
      is ErrorState.LoginRequired -> {
        course?.let {
          if (CoursesStorage.getInstance().hasCourse(it)) {
            return
          }
        }
        setError(message)
      }
      else -> {
        setError(message)
      }
    }
    showErrorPanel()
  }

  private fun showErrorPanel() {
    errorComponent.isVisible = true
    revalidate()
    repaint()
  }

  private fun setError(message: ValidationMessage) {
    errorComponent.setErrorMessage(message)
    header.setButtonToolTip(message.beforeLink + message.linkText + message.afterLink)
  }

  private fun canStartCourse(): Boolean = errorState.courseCanBeStarted

  companion object {
    // default divider's color too dark in Darcula, so use the same color as in plugins dialog
    val DIVIDER_COLOR = JBColor(0xC5C5C5, 0x515151)
  }

}

