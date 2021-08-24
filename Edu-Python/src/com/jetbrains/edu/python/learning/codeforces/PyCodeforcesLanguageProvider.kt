package com.jetbrains.edu.python.learning.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.EduNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.python.learning.PyNewConfigurator
import com.jetbrains.python.newProject.PyNewProjectSettings
import javax.swing.Icon

class PyCodeforcesLanguageProvider : CodeforcesLanguageProvider {
  override val codeforcesLanguageNamings: List<String> = listOf("Python 2", "PyPy 2", "Python 3", "PyPy 3")
  override val configurator: EduConfigurator<PyNewProjectSettings> = PyNewConfigurator()
  override val languageId: String = EduNames.PYTHON
  override val preferableCodeforcesLanguage: String = "Python 3"
  override val templateFileName: String = "codeforces.Python main.py"
  override val displayTemplateName: String = "main.py"
  override val languageIcon: Icon = EducationalCoreIcons.PythonLogo

  override fun getLanguageVersion(codeforcesLanguage: String): String? =
    when (codeforcesLanguage.split(" ").last()) {
      "2" -> PYTHON_2_VERSION
      "3" -> PYTHON_3_VERSION
      else -> null
    }
}