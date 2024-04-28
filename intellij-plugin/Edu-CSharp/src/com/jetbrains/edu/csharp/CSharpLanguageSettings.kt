package com.jetbrains.edu.csharp

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateSdk
import java.awt.BorderLayout
import javax.swing.JComponent

class CSharpLanguageSettings : LanguageSettings<CSharpProjectSettings>() {
  private var version: String = ProjectTemplateSdk.net8.presentation
  override fun getSettings(): CSharpProjectSettings = CSharpProjectSettings(version)
  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {

    val versions = arrayOf(ProjectTemplateSdk.net8.presentation, ProjectTemplateSdk.net7.presentation)
    val langStandardComboBox = ComboBox(versions)
    val courseSdkVersion = course.languageVersion
    if (courseSdkVersion != null && versions.contains(courseSdkVersion)) {
      version = courseSdkVersion
    }
    langStandardComboBox.selectedItem = version

    langStandardComboBox.addItemListener {
      version = it.item.toString()
      notifyListeners()
    }
    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(langStandardComboBox, CSHARP_SDK_PREFIX, BorderLayout.WEST))
  }

  companion object {
    private const val CSHARP_SDK_PREFIX = ".NET SDK"
  }

}