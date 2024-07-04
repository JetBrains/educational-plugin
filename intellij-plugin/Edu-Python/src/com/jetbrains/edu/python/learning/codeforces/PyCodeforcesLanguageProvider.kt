package com.jetbrains.edu.python.learning.codeforces

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYTHON
import com.jetbrains.edu.python.learning.PyNewConfigurator
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings
import javax.swing.Icon

class PyCodeforcesLanguageProvider : CodeforcesLanguageProvider() {
  override val configurator: EduConfigurator<PyProjectSettings> = PyNewConfigurator()
  override val languageId: String = PYTHON
  override val templateFileName: String = "codeforces.Python main.py"
  override val displayTemplateName: String = "main.py"
  override val languageIcon: Icon = EducationalCoreIcons.PythonLogo

}