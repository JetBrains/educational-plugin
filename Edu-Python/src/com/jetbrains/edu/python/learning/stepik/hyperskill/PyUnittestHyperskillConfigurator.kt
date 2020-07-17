package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils.isAndroidStudio
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.MAIN_PY
import com.jetbrains.edu.python.learning.PyNewConfigurator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyUnittestHyperskillConfigurator : HyperskillConfigurator<PyNewProjectSettings>(object : PyNewConfigurator() {
  override fun getMockFileName(text: String): String = MAIN_PY
}) {
  override val testDirs: List<String> = listOf(EduNames.TEST)
  override val isEnabled: Boolean = !isAndroidStudio()
  override val isCourseCreatorEnabled: Boolean = true
}