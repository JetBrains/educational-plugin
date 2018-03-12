package com.jetbrains.edu.python.coursecreator

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.ui.CCEditCourseInfoDialog
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.CoursePanel
import com.jetbrains.edu.python.learning.PyConfigurator.PYTHON_2
import com.jetbrains.edu.python.learning.PyConfigurator.PYTHON_3
import com.jetbrains.python.PythonLanguage
import com.jetbrains.python.psi.LanguageLevel

class PyCCEditCourseInfoDialog(project: Project, course: Course, title: String) : CCEditCourseInfoDialog(project, course, title) {
  private val ALL_VERSIONS = "All versions"

  override fun setVersion(course: Course, panel: CoursePanel) {
    if (PythonLanguage.getInstance() == course.languageById) {
      val version = panel.languageVersion
      var language = PythonLanguage.getInstance().id
      if (version != null && ALL_VERSIONS != version) {
        language += " " + version
      }
      course.language = language
    }
  }

  override fun setupLanguageLevels(course: Course, panel: CoursePanel) {
    if (PythonLanguage.getInstance() == course.languageById) {
      val currentVersion = if (course.languageVersion != null) course.languageVersion else ALL_VERSIONS
      val versions = arrayOf(ALL_VERSIONS, PYTHON_3, PYTHON_2, LanguageLevel.values()).asList()
      panel.setUpLanguageLevels("Python", versions as MutableList<String>?, currentVersion)
    }
  }
}