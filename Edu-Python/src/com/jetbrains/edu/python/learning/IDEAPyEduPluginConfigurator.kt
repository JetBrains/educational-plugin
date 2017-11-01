package com.jetbrains.edu.python.learning

import com.intellij.util.PlatformUtils
import com.jetbrains.edu.learning.EduPluginConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.EduCourseProjectGenerator
import com.jetbrains.python.newProject.PyNewProjectSettings

class IDEAPyEduPluginConfigurator : PyEduPluginConfigurator() {

  override fun getLanguageSettings(): EduPluginConfigurator.LanguageSettings<PyNewProjectSettings> =
          IDEAPyLanguageSettings()

  override fun getEduCourseProjectGenerator(course: Course): EduCourseProjectGenerator<PyNewProjectSettings>? =
          IDEAPyDirectoryProjectGenerator(course)

  override fun isEnabled(): Boolean = !(PlatformUtils.isPyCharm() || PlatformUtils.isCLion())
}
