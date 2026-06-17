package com.jetbrains.edu.learning.newproject.ui.courseSettings

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.CollapsibleRow
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.ModalityStateProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.newproject.nameToLocation
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseBindData
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseSelectionListener
import com.jetbrains.edu.learning.newproject.ui.coursePanel.DESCRIPTION_AND_SETTINGS_TOP_OFFSET
import com.jetbrains.edu.learning.newproject.ui.coursePanel.HORIZONTAL_MARGIN
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.event.DocumentListener

class CourseSettingsPanel(
  private val parentDisposable: Disposable,
  isLocationFieldNeeded: Boolean = false,
  panelTitle: String = EduCoreBundle.message("course.dialog.settings")
) : Wrapper(), CourseSelectionListener {
  var languageSettings: LanguageSettings<*>? = null
  val locationString: String?
    get() = locationField?.component?.text

  var locationField: LabeledComponent<TextFieldWithBrowseButton>? = null
  private val context: UserDataHolder = UserDataHolderBase()
  val settingsPanel = JPanel()

  private var languageSettingsDisposable: CheckedDisposable? = null

  private lateinit var collapsibleGroup: CollapsibleRow

  init {
    border = JBUI.Borders.empty(DESCRIPTION_AND_SETTINGS_TOP_OFFSET, HORIZONTAL_MARGIN, 0, 0)
    settingsPanel.layout = BoxLayout(settingsPanel, BoxLayout.Y_AXIS)
    settingsPanel.border = JBUI.Borders.empty(0, IdeBorderFactory.TITLED_BORDER_INDENT, 5, 0)
    isOpaque = true

    val panel = panel {
      collapsibleGroup = collapsibleGroup(panelTitle) {
        row {
          cell(settingsPanel)
            .align(Align.FILL)
        }
      }.apply {
        packWindowHeight = true
      }
    }.apply {
      isOpaque = false
    }

    setContent(panel)

    if (isLocationFieldNeeded) {
      locationField = createLocationComponent()
    }
  }

  fun setSettingsComponents(settings: List<LabeledComponent<*>>) {
    settingsPanel.removeAll()
    for (setting in settings) {
      settingsPanel.add(setting, BorderLayout.PAGE_END)
    }
    UIUtil.mergeComponentsWithAnchor(settings)
    UIUtil.setBackgroundRecursively(settingsPanel, background)
    settingsPanel.revalidate()
    settingsPanel.repaint()
  }

  fun setOn(on: Boolean) {
    collapsibleGroup.expanded = on
  }

  private fun createLocationComponent(): LabeledComponent<TextFieldWithBrowseButton> {
    val field = TextFieldWithBrowseButton()
    val fileChooserDescriptor = FileChooserDescriptorFactory.singleDir()
      .withTitle(EduCoreBundle.message("action.select.course.location.title"))
      .withDescription(EduCoreBundle.message("action.select.course.location.description"))
    field.addBrowseFolderListener(null, fileChooserDescriptor)
    return LabeledComponent.create(field, EduCoreBundle.message("action.select.course.location"), BorderLayout.WEST)
  }

  fun addLocationFieldDocumentListener(listener: DocumentListener) {
    locationField?.component?.textField?.document?.addDocumentListener(listener)
  }

  fun removeLocationFieldDocumentListener(listener: DocumentListener) {
    locationField?.component?.textField?.document?.removeDocumentListener(listener)
  }

  override fun onCourseSelectionChanged(data: CourseBindData) {
    val (course, courseDisplaySettings) = data
    languageSettingsDisposable?.let(Disposer::dispose)
    val settingsDisposable = Disposer.newCheckedDisposable(parentDisposable, "languageSettingsDisposable")
    languageSettingsDisposable = settingsDisposable

    val settingsComponents = mutableListOf<LabeledComponent<*>>()
    locationField?.let {
      it.component.text = course.nameToLocation()
      settingsComponents.add(it)
    }

    val configurator = course.configurator
    languageSettings = configurator?.courseBuilder?.getLanguageSettings()?.apply {
      if (courseDisplaySettings.showLanguageSettings) {
        val components = getLanguageSettingsComponents(course, ModalityStateProvider.forComponent(this@CourseSettingsPanel), settingsDisposable, context)
        settingsComponents.addAll(components)
      }
    }

    if (settingsComponents.isNotEmpty() && !CoursesStorage.getInstance().hasCourse(course)) {
      isVisible = true
      setSettingsComponents(settingsComponents)
    }
    else {
      isVisible = false
    }
  }

  fun getProjectSettings(): EduProjectSettings? = languageSettings?.getSettings()

  fun validateSettings(course: Course?): SettingsValidationResult {
    val settingsValidationResult = languageSettings?.validate(course, locationString) ?: SettingsValidationResult.OK
    if (settingsValidationResult is SettingsValidationResult.Ready && settingsValidationResult.validationMessage != null) {
      setOn(true)
    }

    return settingsValidationResult
  }
}
