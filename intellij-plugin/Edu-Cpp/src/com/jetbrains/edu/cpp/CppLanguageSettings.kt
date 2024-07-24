package com.jetbrains.edu.cpp

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.UserDataHolder
import com.intellij.util.io.IOUtil
import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard.CPP11
import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard.CPP14
import com.jetbrains.edu.cpp.messages.EduCppBundle
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.stepik.StepikCourse
import com.jetbrains.edu.learning.newproject.ui.errors.SettingsValidationResult
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessageType.WARNING
import com.jetbrains.edu.learning.newproject.ui.errors.ready
import java.awt.BorderLayout
import javax.swing.JComponent

class CppLanguageSettings : LanguageSettings<CppProjectSettings>() {
  private var languageStandard: String = CPP14.standard

  override fun getSettings(): CppProjectSettings = CppProjectSettings(languageStandard)

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    val standards = if (course is StepikCourse) {
      arrayOf(CPP11.standard, CPP14.standard)
    }
    else {
      getLanguageVersions().toTypedArray()
    }

    val langStandardComboBox = ComboBox(standards)
    val courseLanguageStandard = course.languageVersion
    if (courseLanguageStandard != null && standards.contains(courseLanguageStandard)) {
      languageStandard = courseLanguageStandard
    }
    langStandardComboBox.selectedItem = languageStandard

    langStandardComboBox.addItemListener {
      languageStandard = it.item.toString()
      notifyListeners()
    }

    return listOf<LabeledComponent<JComponent>>(LabeledComponent.create(langStandardComboBox, CPP_STANDARD_PREFIX, BorderLayout.WEST))
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    return if (courseLocation != null && SystemInfo.isWindows && !IOUtil.isAscii(courseLocation)) {
      ValidationMessage(EduCppBundle.message("error.non.ascii"), null, WARNING).ready()
    }
    else {
      SettingsValidationResult.OK
    }
  }

  companion object {
    private const val CPP_STANDARD_PREFIX = "C++ Standard"
  }
}
