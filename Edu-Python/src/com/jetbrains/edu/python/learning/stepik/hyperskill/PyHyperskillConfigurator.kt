package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils.isAndroidStudio
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings

class PyHyperskillConfigurator : HyperskillConfigurator<PyProjectSettings>(PyHyperskillBaseConfigurator()) {
  override val testDirs: List<String> = listOf(HYPERSKILL_TEST_DIR, EduNames.TEST)
  override val isEnabled: Boolean = !isAndroidStudio()
  override val isCourseCreatorEnabled: Boolean = true
}
