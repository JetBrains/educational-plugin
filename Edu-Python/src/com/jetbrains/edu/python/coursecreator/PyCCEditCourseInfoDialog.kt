package com.jetbrains.edu.python.coursecreator

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.ui.CCCourseInfoPanel
import com.jetbrains.edu.coursecreator.ui.CCEditCourseInfoDialog
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.PyConfigurator.PYTHON_2
import com.jetbrains.edu.python.learning.PyConfigurator.PYTHON_3
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.LanguageLevel

class PyCCEditCourseInfoDialog(project: Project, course: Course, title: String) : CCEditCourseInfoDialog(project, course, title) {
  private val ALL_VERSIONS = "All versions"

  override fun setVersion(course: Course, panel: CCCourseInfoPanel) {
    if (PythonLanguage.getInstance() == course.languageById) {
      val version = panel.languageVersion
      var language = PythonLanguage.getInstance().id
      if (version != null && ALL_VERSIONS != version) {
        language += " " + version
      }
      course.language = language
    }
  }

  override fun setupLanguageLevels(course: Course, panel: CCCourseInfoPanel) {
    if (PythonLanguage.getInstance() == course.languageById) {
      val languageLevelLabel = panel.languageLevelLabel
      languageLevelLabel.text = "Python:"
      languageLevelLabel.isVisible = true
      val languageLevelCombobox = panel.languageLevelCombobox
      languageLevelCombobox.addItem(ALL_VERSIONS)
      languageLevelCombobox.addItem(PYTHON_3)
      languageLevelCombobox.addItem(PYTHON_2)
      for (level in LanguageLevel.values()) {
        languageLevelCombobox.addItem(level.toString())
      }
      languageLevelCombobox.isVisible = true
      val version = course.languageVersion
      if (version != null) {
        languageLevelCombobox.setSelectedItem(version)
      }
      else {
        languageLevelCombobox.setSelectedItem(ALL_VERSIONS)
      }
    }
  }
}