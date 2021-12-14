package com.jetbrains.edu.python.learning.checkio

import com.intellij.openapi.project.Project
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checkio.CheckiOConnectorProvider
import com.jetbrains.edu.learning.checkio.CheckiOCourseContentGenerator
import com.jetbrains.edu.learning.checkio.checker.CheckiOTaskChecker
import com.jetbrains.edu.learning.checkio.connectors.CheckiOOAuthConnector
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOCourse
import com.jetbrains.edu.learning.checkio.utils.CheckiOCourseGenerationUtils.getCourseFromServerUnderProgress
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.python.learning.PyConfigurator
import com.jetbrains.edu.python.learning.checker.PyEnvironmentChecker
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOApiConnector
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector
import com.jetbrains.edu.python.learning.checkio.utils.PyCheckiONames.*
import com.jetbrains.python.PythonFileType
import com.jetbrains.python.newProject.PyNewProjectSettings
import javax.swing.Icon

class PyCheckiOConfigurator : PyConfigurator(), CheckiOConnectorProvider {
  override val courseBuilder: EduCourseBuilder<PyNewProjectSettings>
    get() = PyCheckiOCourseBuilder()

  override val taskCheckerProvider: TaskCheckerProvider = object : TaskCheckerProvider {
    override val envChecker: EnvironmentChecker
      get() = PyEnvironmentChecker()

    override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
      return CheckiOTaskChecker(task, envChecker, project, PyCheckiOOAuthConnector, PY_CHECKIO_INTERPRETER,
                                PY_CHECKIO_TEST_FORM_TARGET_URL)
    }
  }

  override fun getOAuthConnector(): CheckiOOAuthConnector = PyCheckiOOAuthConnector

  override val logo: Icon
    get() = EducationalCoreIcons.CheckiO

  override val isCourseCreatorEnabled: Boolean
    get() = false

  override fun beforeCourseStarted(course: Course) {
    val contentGenerator = CheckiOCourseContentGenerator(PythonFileType.INSTANCE, PyCheckiOApiConnector.getInstance())
    getCourseFromServerUnderProgress(contentGenerator, (course as CheckiOCourse), PyCheckiOSettings.INSTANCE.account, PY_CHECKIO_API_HOST)
  }

  override val isEnabled: Boolean
    get() = !EduUtils.isAndroidStudio()
}