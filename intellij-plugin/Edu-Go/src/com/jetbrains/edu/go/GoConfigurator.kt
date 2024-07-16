package com.jetbrains.edu.go

import com.intellij.openapi.project.Project
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.go.checker.GoCodeExecutor
import com.jetbrains.edu.go.checker.GoEduTaskChecker
import com.jetbrains.edu.go.checker.GoEnvironmentChecker
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames.TEST
import com.jetbrains.edu.learning.checker.CodeExecutor
import com.jetbrains.edu.learning.checker.EnvironmentChecker
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import javax.swing.Icon

class GoConfigurator : EduConfigurator<GoProjectSettings> {
  override val courseBuilder: EduCourseBuilder<GoProjectSettings>
    get() = GoCourseBuilder()

  override val testFileName: String
    get() = TEST_GO

  override fun getMockFileName(course: Course, text: String): String = TASK_GO

  override val testDirs: List<String>
    get() = listOf(TEST)

  override val logo: Icon
    get() = EducationalCoreIcons.GoLogo

  override val taskCheckerProvider: TaskCheckerProvider
    get() = object : TaskCheckerProvider {
      override val codeExecutor: CodeExecutor
        get() = GoCodeExecutor()
      override val envChecker: EnvironmentChecker
        get() = GoEnvironmentChecker()
      override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = GoEduTaskChecker(project, envChecker, task)
    }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val TEST_GO = "task_test.go"
    const val TASK_GO = "task.go"
    const val MAIN_GO = "main.go"
    const val GO_MOD = "go.mod"
  }
}
