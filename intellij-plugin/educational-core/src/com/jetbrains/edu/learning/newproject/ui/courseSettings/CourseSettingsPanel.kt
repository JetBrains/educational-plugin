package com.jetbrains.edu.learning.newproject.ui.courseSettings

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.PathUtil
import com.intellij.util.io.IOUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.newproject.HyperskillCourseAdvertiser
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseBindData
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseSelectionListener
import com.jetbrains.edu.learning.newproject.ui.coursePanel.DESCRIPTION_AND_SETTINGS_TOP_OFFSET
import com.jetbrains.edu.learning.newproject.ui.coursePanel.HORIZONTAL_MARGIN
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import java.awt.BorderLayout
import java.io.File
import java.text.DateFormat
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.event.DocumentListener

class CourseSettingsPanel(
  private val parentDisposable: Disposable,
  isLocationFieldNeeded: Boolean = false,
  panelTitle: String = EduCoreBundle.message("course.dialog.settings")
) : NonOpaquePanel(), CourseSelectionListener {
  var languageSettings: LanguageSettings<*>? = null
  val locationString: String?
    get() = locationField?.component?.text

  var locationField: LabeledComponent<TextFieldWithBrowseButton>? = null
  private val context: UserDataHolder = UserDataHolderBase()
  val settingsPanel = JPanel()
  private var decorator: HideableNoLineDecorator

  private var languageSettingsDisposable: CheckedDisposable? = null

  init {
    border = JBUI.Borders.empty(DESCRIPTION_AND_SETTINGS_TOP_OFFSET, HORIZONTAL_MARGIN, 0, 0)
    settingsPanel.layout = BoxLayout(settingsPanel, BoxLayout.Y_AXIS)
    settingsPanel.border = JBUI.Borders.empty(0, IdeBorderFactory.TITLED_BORDER_INDENT, 5, 0)
    add(settingsPanel, BorderLayout.CENTER)
    decorator = HideableNoLineDecorator(this, panelTitle)
    decorator.setContentComponent(settingsPanel)

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
    decorator.setOn(on)
  }

  private fun createLocationComponent(): LabeledComponent<TextFieldWithBrowseButton> {
    val field = TextFieldWithBrowseButton()
    field.addBrowseFolderListener(EduCoreBundle.message("action.select.course.location.title"),
                                  EduCoreBundle.message("action.select.course.location.description"),
                                  null,
                                  FileChooserDescriptorFactory.createSingleFolderDescriptor())
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
      it.component.text = nameToLocation(course)
      settingsComponents.add(it)
    }

    val configurator = course.configurator
    languageSettings = configurator?.courseBuilder?.getLanguageSettings()?.apply {
      if (courseDisplaySettings.showLanguageSettings) {
        val components = getLanguageSettingsComponents(course, settingsDisposable, context)
        settingsComponents.addAll(components)
      }
    }

    if (settingsComponents.isNotEmpty()
        && course !is HyperskillCourseAdvertiser
        && !CoursesStorage.getInstance().hasCourse(course)) {
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

  companion object {
    fun nameToLocation(course: Course): String {
      val courseName = course.name
      val language = course.languageDisplayName
      val humanLanguage = course.humanLanguage
      var name = courseName
      if (!IOUtil.isAscii(name)) {
        //there are problems with venv creation for python course
        name = "${EduNames.COURSE} $language $humanLanguage".capitalize()
      }
      if (!PathUtil.isValidFileName(name)) {
        DateFormat.getDateInstance(DateFormat.DATE_FIELD, Locale.getDefault()).format(course.updateDate)
        name = FileUtil.sanitizeFileName(name)
      }
      return FileUtil.findSequentNonexistentFile(File(ProjectUtil.getBaseDir()), name, "").absolutePath
    }
  }
}
