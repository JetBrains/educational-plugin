package com.jetbrains.edu.cpp

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CMakeCPPProjectGenerator
import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import java.awt.BorderLayout
import javax.swing.JComponent

class CppLanguageSettings : LanguageSettings<CppProjectSettings>() {

  private var languageVersion: String = CMakeRecognizedCPPLanguageStandard.CPP14.displayString

  override fun getSettings(): CppProjectSettings = CppProjectSettings(languageVersion)

  override fun getLanguageSettingsComponents(course: Course): List<LabeledComponent<JComponent>> {
    val langStandardComboBox = ComboBox(CMakeCPPProjectGenerator().languageVersions)
    langStandardComboBox.selectedItem = languageVersion

    langStandardComboBox.addItemListener {
      languageVersion = it.item.toString()
      notifyListeners()
    }

    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(langStandardComboBox, "Language Standard", BorderLayout.WEST))
  }
}