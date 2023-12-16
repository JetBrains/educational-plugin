package com.jetbrains.edu.python.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtilsKt.isAndroidStudio
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import com.jetbrains.edu.python.learning.PyConfigurator.Companion.MAIN_PY
import com.jetbrains.edu.python.learning.PyNewConfigurator
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings

class PyUnittestHyperskillConfigurator : HyperskillConfigurator<PyProjectSettings>(PyNewConfigurator()) {
  override val testDirs: List<String> = listOf(EduNames.TEST)
  override val isEnabled: Boolean = !isAndroidStudio()
  override val isCourseCreatorEnabled: Boolean = true

  override fun getMockFileName(course: Course, text: String): String = MAIN_PY
}