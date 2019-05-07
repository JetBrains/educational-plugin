package com.jetbrains.edu.cpp

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import java.awt.BorderLayout
import javax.swing.JComponent

class CppLanguageSettings : LanguageSettings<CppProjectSettings>() {

  private var languageStandard: String = CMakeRecognizedCPPLanguageStandard.CPP14.standard

  override fun getSettings(): CppProjectSettings = CppProjectSettings(languageStandard)

  override fun getLanguageSettingsComponents(course: Course): List<LabeledComponent<JComponent>> {
    val standards = if (course.isStudy) arrayOf(languageStandard) else languageVersions.toTypedArray()

    val langStandardComboBox = ComboBox(standards)
    langStandardComboBox.selectedItem = languageStandard

    langStandardComboBox.addItemListener {
      languageStandard = it.item.toString()
      notifyListeners()
    }

    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(langStandardComboBox, "C++ Standard", BorderLayout.WEST))
  }

  override fun getLanguageVersions(): List<String> {
    return CMakeRecognizedCPPLanguageStandard.values().map { it.standard }
  }
}
