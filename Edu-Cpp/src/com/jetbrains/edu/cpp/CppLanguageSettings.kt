package com.jetbrains.edu.cpp

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard.CPP11
import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard.CPP14
import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard.values
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import java.awt.BorderLayout
import javax.swing.JComponent

class CppLanguageSettings : LanguageSettings<CppProjectSettings>() {

  private var languageStandard: String = CPP14.standard

  override fun getSettings(): CppProjectSettings = CppProjectSettings(languageStandard)

  override fun getLanguageSettingsComponents(course: Course): List<LabeledComponent<JComponent>> {
    val standards = when(course) {
      is StepikCourse -> arrayOf(CPP11.standard, languageStandard)
      else -> languageVersions.toTypedArray()
    }

    val langStandardComboBox = ComboBox(standards)
    langStandardComboBox.selectedItem = languageStandard

    langStandardComboBox.addItemListener {
      languageStandard = it.item.toString()
      notifyListeners()
    }

    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(langStandardComboBox, "C++ Standard", BorderLayout.WEST))
  }

  override fun getLanguageVersions(): List<String> {
    return values().map { it.standard }
  }
}
