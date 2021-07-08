package com.jetbrains.edu.learning.newproject.ui.courseSettings

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.PathUtil
import com.intellij.util.io.IOUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog.Companion.IS_LOCAL_COURSE_KEY
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.coursePanel.*
import java.awt.BorderLayout
import java.io.File
import java.text.DateFormat
import java.util.*
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.event.DocumentListener

class CourseSettingsPanel(isLocationFieldNeeded: Boolean = false) : NonOpaquePanel(), CourseSelectionListener {
  var languageSettings: LanguageSettings<*>? = null
  val locationString: String?
    get() = locationField?.component?.text

  private var locationField: LabeledComponent<TextFieldWithBrowseButton>? = null
  private val context: UserDataHolder = UserDataHolderBase()
  private val settingsPanel = JPanel()
  private var decorator: HideableNoLineDecorator

  init {
    border = JBUI.Borders.empty(DESCRIPTION_AND_SETTINGS_TOP_OFFSET, HORIZONTAL_MARGIN, 0, 0)
    settingsPanel.layout = BoxLayout(settingsPanel, BoxLayout.Y_AXIS)
    settingsPanel.border = JBUI.Borders.empty(0, IdeBorderFactory.TITLED_BORDER_INDENT, 5, 0)
    add(settingsPanel, BorderLayout.CENTER)
    decorator = HideableNoLineDecorator(this, EduCoreBundle.message("course.dialog.settings"))
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
    field.addBrowseFolderListener("Select Course Location",
                                  "Select course location",
                                  null,
                                  FileChooserDescriptorFactory.createSingleFolderDescriptor())
    return LabeledComponent.create(field, "Location", BorderLayout.WEST)
  }

  fun addLocationFieldDocumentListener(listener: DocumentListener) {
    locationField?.component?.textField?.document?.addDocumentListener(listener)
  }

  fun removeLocationFieldDocumentListener(listener: DocumentListener) {
    locationField?.component?.textField?.document?.removeDocumentListener(listener)
  }

  override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
    val course = courseInfo.course
    val settingsComponents = mutableListOf<LabeledComponent<*>>()
    locationField?.let {
      it.component.text = nameToLocation(course)
      settingsComponents.add(it)
    }

    val configurator = course.configurator
    if (configurator != null) {
      languageSettings = configurator.courseBuilder.getLanguageSettings().apply {
        if (courseDisplaySettings.showLanguageSettings) {
          val components = getLanguageSettingsComponents(course, context)
          settingsComponents.addAll(components)
        }
      }
    }

    if (settingsComponents.isNotEmpty()
        && course !is JetBrainsAcademyCourse
        && course !is CodeforcesCourse
        && (!CoursesStorage.getInstance().hasCourse(course))
        || course.dataHolder.getUserData(IS_LOCAL_COURSE_KEY) == true) {
      isVisible = true
      setSettingsComponents(settingsComponents)
    }
    else {
      isVisible = false
    }
  }

  fun getProjectSettings(): Any? = languageSettings?.settings

  fun validateSettings(course: Course?): ValidationMessage? {
    val validationMessage = languageSettings?.validate(course, locationString) ?: return null

    setOn(true)
    return validationMessage
  }

  companion object {
    fun nameToLocation(course: Course): String {
      val courseName = course.name
      val language = course.languageDisplayName
      val humanLanguage = course.humanLanguage
      var name = courseName
      if (!IOUtil.isAscii(name!!)) {
        //there are problems with venv creation for python course
        name = "${EduNames.COURSE} $language $humanLanguage".capitalize()
      }
      if (!PathUtil.isValidFileName(name)) {
        DateFormat.getDateInstance(DateFormat.DATE_FIELD, Locale.getDefault()).format(course.updateDate)
        name = FileUtil.sanitizeFileName(name)
      }
      return FileUtil.findSequentNonexistentFile(File(ProjectUtil.getBaseDir()), name, "").absolutePath
    }

    fun getLanguageSettings(course: Course): LanguageSettings<out Any?>? = course.configurator?.courseBuilder?.getLanguageSettings()
  }
}