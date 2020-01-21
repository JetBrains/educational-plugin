package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduUtils.isAndroidStudio
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.python.newProject.PyNewProjectSettings

class PyHyperskillConfigurator : HyperskillConfigurator<PyNewProjectSettings>(PyConfigurator()) {
  override fun getTestDirs() = listOf(HYPERSKILL_TEST_DIR)
  override fun isEnabled(): Boolean = !isAndroidStudio()
}
