package com.jetbrains.edu.learning.newproject.ui.courseSettings

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import com.intellij.util.io.IOUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.coursePanel.DESCRIPTION_AND_SETTINGS_TOP_OFFSET
import java.awt.BorderLayout
import java.io.File
import java.text.DateFormat
import java.util.*
import javax.swing.event.DocumentListener

// Merge with base class when new ui is implemented and rename to `CourseSettings`
class NewCourseSettings(isLocationFieldNeeded: Boolean, leftMargin: Int) : CourseSettings() {
  private var myLocationField: LabeledComponent<TextFieldWithBrowseButton>? = null
  var languageSettings: LanguageSettings<*>? = null
  private val context: UserDataHolder = UserDataHolderBase()

  init {
    border = JBUI.Borders.empty(DESCRIPTION_AND_SETTINGS_TOP_OFFSET, leftMargin, 0, 0)
    if (isLocationFieldNeeded) {
      myLocationField = createLocationComponent()
    }

    UIUtil.setBackgroundRecursively(this, UIUtil.getEditorPaneBackground())
  }

  private fun createLocationComponent(): LabeledComponent<TextFieldWithBrowseButton> {
    val field = TextFieldWithBrowseButton()
    field.addBrowseFolderListener("Select Course Location",
                                  "Select course location",
                                  null,
                                  FileChooserDescriptorFactory.createSingleFolderDescriptor())
    return LabeledComponent.create(field, "Location", BorderLayout.WEST)
  }

  val locationString: String?
    get() = myLocationField?.component?.text

  fun addLocationFieldDocumentListener(listener: DocumentListener) {
    myLocationField?.component?.textField?.document?.addDocumentListener(listener)
  }

  fun update(course: Course, showLanguageSettings: Boolean) {
    val settingsComponents = mutableListOf<LabeledComponent<*>>()
    myLocationField?.let {
      it.component.text = nameToLocation(course)
      settingsComponents.add(it)
    }

    val configurator = course.configurator
    if (configurator != null) {
      val settings = configurator.courseBuilder.getLanguageSettings()
      languageSettings = settings
      if (showLanguageSettings) {
        val components = settings.getLanguageSettingsComponents(course, context)
        settingsComponents.addAll(components)
      }
    }

    if (settingsComponents.isNotEmpty() && course !is JetBrainsAcademyCourse) {
      isVisible = true
      setSettingsComponents(settingsComponents)
    }
    else {
      isVisible = false
    }
    UIUtil.setBackgroundRecursively(this, UIUtil.getEditorPaneBackground())
  }

  fun getProjectSettings(): Any? = languageSettings?.settings

  fun validateSettings(course: Course?): ValidationMessage? {
    val validationMessage = languageSettings?.validate(course, locationString) ?: return null

    setOn(true)
    return validationMessage
  }

  private fun nameToLocation(course: Course): String {
    val courseName = course.name
    val language = course.languageDisplayName
    val humanLanguage = course.humanLanguage
    var name = courseName
    if (!IOUtil.isAscii(name!!)) {
      //there are problems with venv creation for python course
      name = "${EduNames.COURSE} $language $humanLanguage".capitalize()
    }
    if (!PathUtil.isValidFileName(name)) {
      DateFormat.getDateInstance(DateFormat.DATE_FIELD,
                                 Locale.getDefault()).format(course.updateDate)
      name = FileUtil.sanitizeFileName(name)
    }
    return FileUtil.findSequentNonexistentFile(File(ProjectUtil.getBaseDir()), name, "").absolutePath
  }
}