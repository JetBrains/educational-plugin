package com.jetbrains.edu.python.learning.pycharm

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduPluginConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.python.learning.PyPluginConfigurator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyPluginConfigurator : PyPluginConfigurator() {

  override fun getLanguageSettings(): EduPluginConfigurator.LanguageSettings<PyNewProjectSettings> =
    PyLanguageSettings()

  override fun getEduCourseProjectGenerator(course: Course): CourseProjectGenerator<PyNewProjectSettings>? =
    PyDirectoryProjectGenerator(course)

  override fun isEnabled(): Boolean = PlatformUtils.isPyCharm() || PlatformUtils.isCLion()
}
