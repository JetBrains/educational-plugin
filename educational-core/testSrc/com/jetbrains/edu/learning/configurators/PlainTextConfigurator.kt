package com.jetbrains.edu.learning.configurators

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class PlainTextConfigurator : EduConfigurator<Unit> {

  override fun getCourseBuilder() = PlainTextCourseBuilder()
  override fun getTestFileName() = "Tests.txt"
  override fun isTestFile(project: Project, file: VirtualFile): Boolean = file.name == testFileName

  override fun getTaskCheckerProvider() = TaskCheckerProvider { task, project ->
    object : TaskChecker<EduTask>(task, project) {
      override fun check(indicator: ProgressIndicator): CheckResult = CheckResult(CheckStatus.Solved, "")
    }
  }
}

class PlainTextCourseBuilder : EduCourseBuilder<Unit> {
  override fun getLanguageSettings(): LanguageSettings<Unit> = object : LanguageSettings<Unit>() {
    override fun getSettings() {}
  }
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<Unit> = PlainTextCourseGenerator(
    this, course)
  override fun getTaskTemplateName(): String? = "Task.txt"
  override fun getTestTemplateName(): String? = "Tests.txt"
}

class PlainTextCourseGenerator(builder: EduCourseBuilder<Unit>, course: Course) : CourseProjectGenerator<Unit>(builder, course)
