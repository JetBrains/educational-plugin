package com.jetbrains.edu.shell

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class ShellConfigurator : EduConfigurator<EmptyProjectSettings> {

  override val courseBuilder: EduCourseBuilder<EmptyProjectSettings>
    get() = ShellCourseBuilder()

  override val testFileName: String
    get() = TEST_SH

  override val taskCheckerProvider: TaskCheckerProvider
    get() = ShellTaskCheckerProvider()

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Shell

  override fun getMockFileName(course: Course, text: String): String = TASK_SH

  companion object {
    @NonNls
    const val TASK_SH = "task.sh"
    @NonNls
    const val TEST_SH = "test.sh"
  }
}