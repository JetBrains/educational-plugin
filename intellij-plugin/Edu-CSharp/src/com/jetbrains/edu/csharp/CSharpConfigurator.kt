package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import org.jetbrains.annotations.NonNls

class CSharpConfigurator : EduConfigurator<CSharpProjectSettings> {
  override val courseBuilder: EduCourseBuilder<CSharpProjectSettings>
    get() = CSharpCourseBuilder()

  override val testFileName: String
    get() = TEST_CS

  override val taskCheckerProvider: TaskCheckerProvider
    get() = TODO("Not yet implemented")

  override fun getMockFileName(course: Course, text: String): String = TASK_CS

  companion object {
    @NonNls
    const val TASK_CS = "task.cs"

    @NonNls
    const val TEST_CS = "test.cs"
  }
}