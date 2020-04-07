package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.FilterComponent
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.event.DocumentListener

const val DESCRIPTION_AND_SETTINGS_TOP_OFFSET = 25

private const val HORIZONTAL_MARGIN = 10
private const val LARGE_HORIZONTAL_MARGIN = 15
private const val LINE_BORDER_THICKNESS = 1
private val DIALOG_SIZE = JBUI.size(400, 600)

// TODO: Rename to CoursePanel after CoursePanel.java is removed
class NewCoursePanel(
  val isStandalonePanel: Boolean,
  val isLocationFieldNeeded: Boolean,
  joinCourseAction: (CourseInfo, CourseMode) -> Unit
) : JPanel() {
  private var header = HeaderPanel(leftMargin, joinCourseAction)
  private var description = CourseDescriptionPanel(leftMargin)
  private var advancedSettings = NewCourseSettings(isLocationFieldNeeded, leftMargin)
  private var mySearchField: FilterComponent? = null

  init {
    layout = VerticalFlowLayout(0, 0)

    // We want to show left part of border only if panel is independent
    val leftBorder = if (isStandalonePanel) LINE_BORDER_THICKNESS else 0
    border = JBUI.Borders.customLine(DIVIDER_COLOR, LINE_BORDER_THICKNESS, leftBorder, LINE_BORDER_THICKNESS, 0)

    add(header)
    add(description)
    add(advancedSettings)
    background = UIUtil.getEditorPaneBackground()
  }

  fun setButtonsEnabled(isEnabled: Boolean) {
    header.setButtonsEnabled(isEnabled)
  }

  private val leftMargin: Int
    get() {
      return if (isStandalonePanel) {
        LARGE_HORIZONTAL_MARGIN
      }
      else {
        HORIZONTAL_MARGIN
      }
    }

  override fun getMinimumSize(): Dimension {
    return DIALOG_SIZE
  }

  // to use in JoinCoursePanel
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

  fun bindCourse(course: Course, settings: CourseDisplaySettings = CourseDisplaySettings()): LanguageSettings<*>? {
    isVisible = true
    advancedSettings.update(course, settings.showLanguageSettings)
    updateCourseDescriptionPanel(course, settings)
    return advancedSettings.languageSettings
  }

  fun validateSettings(course: Course?) = advancedSettings.validateSettings(course)

  fun hideContent() {
    isVisible = false
  }

  val locationString: String?
    get() = advancedSettings.locationString

  val projectSettings: Any?
    get() = advancedSettings.getProjectSettings()

  fun bindSearchField(searchField: FilterComponent) {
    mySearchField = searchField
  }

  companion object {
    // default divider's color too dart in Darcula, so use the same color as in plugins dialog
    val DIVIDER_COLOR = JBColor(Gray._192, Gray._81)
  }
}
