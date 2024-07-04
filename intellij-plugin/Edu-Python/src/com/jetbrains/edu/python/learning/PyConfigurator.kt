package com.jetbrains.edu.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.python.learning.checker.PyTaskCheckerProvider
import com.jetbrains.edu.python.learning.newproject.PyProjectSettings
import com.jetbrains.python.psi.icons.PythonPsiApiIcons
import javax.swing.Icon

open class PyConfigurator : EduConfigurator<PyProjectSettings> {
  override val courseBuilder: EduCourseBuilder<PyProjectSettings>
    get() = PyCourseBuilder()

  override fun getMockFileName(course: Course, text: String): String = TASK_PY

  override val testFileName: String
    get() = TESTS_PY

  override fun excludeFromArchive(project: Project, course: Course, file: VirtualFile): Boolean =
    super.excludeFromArchive(project, course, file) || excludeFromArchive(file)

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PyTaskCheckerProvider()

  override val logo: Icon
    get() = PythonPsiApiIcons.Python

  override val isCourseCreatorEnabled: Boolean
    get() = false

  override val defaultPlaceholderText: String
    get() = "# TODO"

  companion object {
    const val TESTS_PY = "tests.py"
    const val TASK_PY = "task.py"
    const val MAIN_PY = "main.py"
  }
}
