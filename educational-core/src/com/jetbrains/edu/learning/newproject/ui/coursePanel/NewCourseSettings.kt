package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.newproject.ui.CourseSettings
import com.jetbrains.edu.learning.newproject.ui.ValidationMessage
import java.awt.BorderLayout
import java.util.*
import javax.swing.event.DocumentListener

// Merge with base class when new ui is implemented and rename to `CourseSettings`
class NewCourseSettings(isLocationFieldNeeded: Boolean, leftMargin: Int) : CourseSettings() {
  private var myLocationField: LabeledComponent<TextFieldWithBrowseButton>? = null
  lateinit var languageSettings: LanguageSettings<*>
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
    val configurator = course.configurator ?: return
    languageSettings = configurator.courseBuilder.getLanguageSettings()
    val settingsComponents: MutableList<LabeledComponent<*>> = ArrayList()
    myLocationField?.let {
      it.component.text = nameToLocation(course)
      settingsComponents.add(it)
    }
    if (showLanguageSettings) {
      val components = languageSettings.getLanguageSettingsComponents(course, context)
      settingsComponents.addAll(components)
    }

    if (settingsComponents.isNotEmpty()) {
      isVisible = true
      setSettingsComponents(settingsComponents)
    }
    else {
      isVisible = false
    }
    UIUtil.setBackgroundRecursively(this, UIUtil.getEditorPaneBackground())
  }

  fun getProjectSettings(): Any = languageSettings.settings

  fun validateSettings(course: Course?): ValidationMessage? {
    val validationMessage = languageSettings.validate(course, locationString) ?: return null

    setOn(true)
    return validationMessage
  }
}