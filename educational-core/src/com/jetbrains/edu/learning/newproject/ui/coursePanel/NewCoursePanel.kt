package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.FilterComponent
import com.intellij.ui.Gray
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.CoursesStorage
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.configuration.CourseCantBeStartedException
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import com.jetbrains.edu.learning.newproject.ui.JoinCourseDialogBase
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettings
import com.jetbrains.edu.learning.stepik.hyperskill.courseGeneration.HyperskillProjectAction
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import java.awt.BorderLayout
import java.awt.CardLayout
import java.util.*
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
import javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
import javax.swing.event.DocumentListener

const val DESCRIPTION_AND_SETTINGS_TOP_OFFSET = 25

private const val ERROR_LABEL_TOP_GAP = 20
private const val HORIZONTAL_MARGIN = 10
private const val LARGE_HORIZONTAL_MARGIN = 15

private const val EMPTY = "empty"
private const val CONTENT = "content"

private const val BEFORE_LINK = "beforeLink"
private const val LINK = "link"
private const val LINK_TEXT = "linkText"
private const val AFTER_LINK = "afterLink"
private val LINK_ERROR_PATTERN: Regex = """(?<$BEFORE_LINK>.*)<a href="(?<$LINK>.*)">(?<$LINK_TEXT>.*)</a>(?<$AFTER_LINK>.*)""".toRegex()
private fun MatchGroupCollection.valueOrEmpty(groupName: String): String = this[groupName]?.value ?: ""


// TODO: Rename to CoursePanel after CoursePanel.java is removed
class NewCoursePanel(
  val isStandalonePanel: Boolean,
  val isLocationFieldNeeded: Boolean,
  val joinCourseAction: ((CourseInfo, CourseMode) -> Unit)? = null
) : JPanel() {
  var errorState: ErrorState = ErrorState.NothingSelected
  var course: Course? = null

  private var header = HeaderPanel(leftMargin, joinCourseAction ?: { course, mode -> joinCourse(course, mode) })
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
    layout = CardLayout()

    border = JBUI.Borders.customLine(DIVIDER_COLOR, 0, 0, 0, 0)

    val emptyStatePanel = JBPanelWithEmptyText().withEmptyText(EduCoreBundle.message("course.dialog.no.course.selected"))
    add(emptyStatePanel, EMPTY)

    val content = JPanel(VerticalFlowLayout(0, 0))
    content.add(header)
    content.add(description)
    content.add(advancedSettings)
    content.add(createErrorPanel())
    val scrollPane = JBScrollPane(content, VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER).apply {
      border = null
    }
    add(scrollPane, CONTENT)

    background = UIUtil.getEditorPaneBackground()
  }

  fun setButtonsEnabled(isEnabled: Boolean) {
    header.setButtonsEnabled(isEnabled)
  }

  fun addLocationFieldDocumentListener(listener: DocumentListener) {
    advancedSettings.addLocationFieldDocumentListener(listener)
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
    header.update(CourseInfo(course, { locationString }, { projectSettings }), settings)
    description.bind(course)
  }

  private fun createErrorPanel(): JPanel {
    val errorPanel = JPanel(BorderLayout())
    errorPanel.add(errorLabel, BorderLayout.CENTER)
    errorPanel.border = JBUI.Borders.empty(ERROR_LABEL_TOP_GAP, leftMargin, 0, 0)
    addErrorStateListener()
    UIUtil.setBackgroundRecursively(errorPanel, MAIN_BG_COLOR)
    return errorPanel
  }

  private fun addErrorStateListener() {
    errorLabel.addHyperlinkListener(ErrorStateHyperlinkListener())
  }

  fun bindCourse(course: Course, settings: CourseDisplaySettings = CourseDisplaySettings()): LanguageSettings<*>? {
    (layout as CardLayout).show(this, CONTENT)
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

  private fun joinCourse(courseInfo: CourseInfo, courseMode: CourseMode) {
    val (course, getLocation, getProjectSettings) = courseInfo

    // location is null for course preview dialog only
    val location = getLocation()
    if (location == null) {
      return
    }

    CoursesStorage.getInstance().addCourse(course, location)

    if (course is JetBrainsAcademyCourse) {
      joinJetBrainsAcademy()
      return
    }

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

  private fun joinJetBrainsAcademy() {
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
