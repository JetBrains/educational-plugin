package com.jetbrains.edu.python.learning.stepik.hyperskill.pycharm

import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.python.learning.pycharm.PyLanguageSettings
import com.jetbrains.edu.python.learning.stepik.hyperskill.PyHyperskillCourseBuilder
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyHyperskillCourseBuilder : PyHyperskillCourseBuilder() {
  override fun getLanguageSettings(): LanguageSettings<PyNewProjectSettings> = PyLanguageSettings()
}
