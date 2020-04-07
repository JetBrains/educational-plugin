package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils.isAndroidStudio
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.python.learning.PyNewConfigurator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyUnittestHyperskillConfigurator : HyperskillConfigurator<PyNewProjectSettings>(PyNewConfigurator()) {
  override val testDirs: List<String> = listOf(EduNames.TEST)
  override val isEnabled: Boolean = !isAndroidStudio()
  override val isCourseCreatorEnabled: Boolean = true
}