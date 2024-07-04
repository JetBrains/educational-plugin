package com.jetbrains.edu.python.learning.checkio

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.utils.CheckiOCourseGenerationUtils.getCourseFromServerUnderProgress
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOCourse
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.edu.python.learning.checkio.checker.PyCheckiOTaskCheckerProvider
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames.PY_CHECKIO_URL
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings
import com.jetbrains.python.PythonFileType
import javax.swing.Icon

class PyCheckiOConfigurator : PyConfigurator(), CheckiOConnectorProvider {
  override val courseBuilder: EduCourseBuilder<PyProjectSettings>
    get() = PyCheckiOCourseBuilder()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PyCheckiOTaskCheckerProvider()

  override val logo: Icon
    get() = EducationalCoreIcons.PyCheckiO

  override val isCourseCreatorEnabled: Boolean = false

  override val isEnabled: Boolean
    get() = super.isEnabled && !EduUtilsKt.isAndroidStudio()

  override fun beforeCourseStarted(course: Course) {
    val contentGenerator = CheckiOCourseContentGenerator(PythonFileType.INSTANCE, PyCheckiOApiConnector)
    getCourseFromServerUnderProgress(contentGenerator, (course as CheckiOCourse), PyCheckiOSettings.getInstance().account, PY_CHECKIO_URL)
  }

  override val oAuthConnector: CheckiOOAuthConnector
    get() = PyCheckiOOAuthConnector
}